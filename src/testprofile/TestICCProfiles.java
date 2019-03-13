package testprofile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.PlanarImage;
import javax.media.jai.RegistryMode;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputAdapter;

import net.kenevans.imagemodel.utils.ImageUtils;
import net.kenevans.imagemodel.utils.Utils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * Created on Oct 31, 2012
 * By Kenneth Evans, Jr.
 */

public class TestICCProfiles extends JFrame
{
    public static final String LS = System.getProperty("line.separator");
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Test ICC Profiles";
    private static final String FILENAME = "c:/Users/evans/Pictures/Calibration/ColorChecker_Adobe1998.jpg";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static final String ICC_FILENAME = "C:/Windows/System32/spool/drivers/color/xRite-2012-09-28-6500-2.2-090.icc";

    private static enum ReadMode {
        IMAGEIO, SPECIAL, SPECIAL_METADATA, JAI
    };

    private ReadMode readMode = ReadMode.JAI;

    private static enum WriteMode {
        IMAGEIO, JAI_NOCONVERT, JAI_NOASSIGN, JAI_ASSIGN
    };

    private WriteMode writeMode = WriteMode.JAI_NOASSIGN;

    private String currentDir = null;
    private File file = null;

    private MouseInputAdapter mouseAdapter;

    private ImagePanel imagePanel = new ImagePanel();
    private JPanel mainPanel = new JPanel();
    private JPanel displayPanel = new JPanel();
    private JLabel statusBar;

    private JMenuBar menuBar = new JMenuBar();
    private JMenu menuFile = new JMenu();
    private JMenuItem menuFileOpen = new JMenuItem();
    private JMenuItem menuFileSaveAs = new JMenuItem();
    private JMenuItem menuFileReadMode = new JMenuItem();
    private JMenuItem menuFileWriteMode = new JMenuItem();
    private JMenuItem menuFileRegistry = new JMenuItem();
    private JMenuItem menuInfoImageInfo = new JMenuItem();
    private JMenuItem menuFileExit = new JMenuItem();

    public TestICCProfiles() {
        this.setTitle(TITLE);
        file = new File(FILENAME);
        if(file != null) {
            currentDir = file.getPath();
        }
        BufferedImage image = openFile(file);
        this.setTitle(file.getName());
        imagePanel.setImage(image);

        uiInit();

        menuInit();
    }

    private void uiInit() {
        // Display panel
        displayPanel.setLayout(new BorderLayout());
        displayPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        displayPanel.add(imagePanel);

        // // DEBUG
        // displayPanel.setBackground(Color.RED);
        // imagePanel.setBackground(Color.GREEN);

        // Main panel
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);

        statusBar = new JLabel();
        // Use space to keep it from resizing. Cannot use ""
        statusBar.setText(" ");
        statusBar.setToolTipText("Status");
        statusBar.setBorder(BorderFactory
            .createBevelBorder(BevelBorder.LOWERED));
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private void menuInit() {
        // Menu
        this.setJMenuBar(menuBar);

        // File
        menuFile.setText("File");
        menuBar.add(menuFile);

        // File Open
        menuFileOpen.setText("Open File...");
        menuFileOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                open();
            }
        });
        menuFile.add(menuFileOpen);

        // File Save as
        menuFileSaveAs.setText("Save As...");
        menuFileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            InputEvent.CTRL_DOWN_MASK));
        menuFileSaveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveAs();
            }
        });
        menuFile.add(menuFileSaveAs);

        // File readMode
        menuFileReadMode.setText("Select Read Mode...");
        menuFileReadMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                selectReadMode();
            }
        });
        menuFile.add(menuFileReadMode);

        // File writeMode
        menuFileWriteMode.setText("Select Write Mode...");
        menuFileWriteMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                selectWriteMode();
            }
        });
        menuFile.add(menuFileWriteMode);

        // File Registry
        menuFileRegistry.setText("Dump Registry");
        menuFileRegistry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.out.println();
                listRegistry();
                System.out.println();
           }
        });
        menuFile.add(menuFileRegistry);

        // File Image Info
        menuInfoImageInfo.setText("Image Info...");
        menuInfoImageInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                imageInfo();
            }
        });
        menuFile.add(menuInfoImageInfo);

        // File Exit
        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });
        menuFile.add(menuFileExit);
    }

    /**
     * Updates the status bar with the given text.
     * 
     * @param text
     */
    public void updateStatus(String text) {
        if(text == null) {
            return;
        }
        statusBar.setText(text);
    }

    private static void parseNode(int level, Node node) {
        String singleTab = "  ";
        String tab = "";
        for(int i = 0; i < level; i++) {
            tab += singleTab;
        }
        System.out.println(tab + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        Node node1;
        if(map != null) {
            for(int j = 0; j < map.getLength(); j++) {
                node1 = map.item(j);
                String attrname = node1.getNodeName();
                String attrval = node1.getNodeValue();
                System.out.println(tab + singleTab + attrname + "=" + attrval);
            }
        }
        NodeList nodeList = node.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); i++) {
            node1 = nodeList.item(i);
            parseNode(level + 1, node1);
        }
    }

    /**
     * Shows the image info.
     */
    private void imageInfo() {
        if(imagePanel == null) return;
        String info = getInfo(imagePanel.getImage()) + LS;
        Utils.infoMsg(info);
    }

    /**
     * Gets information for the given BufferedImage.
     * 
     * @param image
     * @return
     */
    public String getInfo(BufferedImage image) {
        String info = "";
        if(image == null) {
            info += "No image";
            return info;
        }
        if(file != null) {
            info += file.getPath() + LS;
            info += file.getName() + LS;
        } else {
            info += "Unknown file" + LS;
        }
        info += LS;
        info += image.getWidth() + " x " + image.getHeight() + LS;
        Map<String, String> types = new HashMap<String, String>();
        types.put("5", "TYPE_3BYTE_BGR");
        types.put("6", "TYPE_4BYTE_ABGR");
        types.put("7", "TYPE_4BYTE_ABGR_PRE");
        types.put("12", "TYPE_BYTE_BINARY");
        types.put("10", "TYPE_BYTE_GRAY");
        types.put("13", "TYPE_BYTE_INDEXED");
        types.put("0", "TYPE_CUSTOM");
        types.put("2", "TYPE_INT_ARGB");
        types.put("3", "TYPE_INT_ARGB_PRE");
        types.put("4", "TYPE_INT_BGR");
        types.put("1", "TYPE_INT_RGB");
        types.put("9", "TYPE_USHORT_555_RGB");
        types.put("8", "TYPE_USHORT_565_RGB");
        types.put("11", "TYPE_USHORT_GRAY");
        Integer type = Integer.valueOf(image.getType());
        String stringType = types.get(type.toString());
        if(stringType == null) stringType = "Unknown";
        info += "Type: " + stringType + " [" + type + "]" + LS;
        info += "Properties:" + LS;
        String[] props = image.getPropertyNames();
        if(props == null) {
            info += "  No properties found" + LS;
        } else {
            for(int i = 0; i < props.length; i++) {
                info += "  " + props[i] + ": " + image.getProperty(props[i])
                    + LS;
            }
        }
        info += "ColorModel:" + LS;
        // The following assumes a particular format for toString()
        String colorModel = image.getColorModel().toString();
        String[] tokens = colorModel.split(" ");
        String colorModelName = tokens[0];
        info += "  " + colorModelName + LS;
        info += "  ";
        for(int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if(token.equals("=")) {
                i++;
                info += "= " + tokens[i] + LS + "  ";
            } else {
                info += token + " ";
            }
        }
        info += LS;

        // Find the ICC profile used
        String desc = ImageUtils.getICCProfileName(image);
        if(desc != null) {
            info += "ICC Profile=" + desc + LS;
        }

        return info;
    }

    /**
     * Opens the given File containing an image and returns the BufferedImage.
     * 
     * @param file
     * @return Image or null on failure.
     */
    private BufferedImage openFile(File file) {
        if(file == null) {
            return null;
        }
        BufferedImage image = null;
        try {
            switch(readMode) {
            case IMAGEIO:
                // Standard imageio
                image = readImageIOImage(file);
                break;
            case SPECIAL:
                // Ignore metadata
                image = readImageSpecial(file, true);
                break;
            case SPECIAL_METADATA:
                // Do not ignore metadata metadata
                image = readImageSpecial(file, false);
                break;
            case JAI:
                // Use JAI
                image = readImageJAI(file);
                break;
            }
        } catch(IOException ex) {
            Utils.excMsg("Cannot open file", ex);
            ex.printStackTrace();
        }
        if(image == null) {
            Utils.errMsg("Falied to load image");
        }
        return image;
    }

    /**
     * Reads the image from the file using ImageIO.read().
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private static BufferedImage readImageIOImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        return image;
    }

    /**
     * Reads the image from the file using JAI.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private static BufferedImage readImageJAI(File file) throws IOException {
        // DEBUG
        System.out.println("readImageJAI: " + file.getPath());
        PlanarImage pi = JAI.create("FileLoad", file.getPath());
        BufferedImage image = pi.getAsBufferedImage();
        String[] propertyNames = pi.getPropertyNames();
        ColorModel cm = pi.getColorModel();
        String profileName = ImageUtils.getICCProfileName(cm);
        System.out.println("Profile Name=" + profileName);
        System.out.println("Image ID=" + pi.getImageID());

        System.out.println("Property Names");
        for(String name : propertyNames) {
            System.out.println("  " + name);
        }

        return image;
    }

    /**
     * Reads the image from the file using ImageIO.read();.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private static BufferedImage readImageSpecial(File file,
        boolean ignoreMetadata) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        if(stream == null) {
            throw new IIOException("Can't create an ImageInputStream!");
        }
        if(stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        // Get the readers
        System.out.println();
        System.out.println("Readers for " + file.getName());
        Iterator<ImageReader> iter1 = ImageIO.getImageReaders(stream);
        int count = 0;
        while(iter1.hasNext()) {
            ;
            ImageReader reader = (ImageReader)iter1.next();
            System.out.println("  " + count++ + " " + reader.getFormatName()
                + " " + reader.getClass().getName());
        }

        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if(!iter.hasNext()) {
            return null;
        }

        ImageReader reader = (ImageReader)iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        // Set it to not ignore metadata
        reader.setInput(stream, true, ignoreMetadata);

        System.out
            .println(file.getName() + " format=" + reader.getFormatName());
        IIOMetadata metadata = reader.getImageMetadata(0);
        if(metadata != null) {
            System.out.println("Metadata Format Names");
            String[] formatNames = metadata.getMetadataFormatNames();
            if(formatNames != null) {
                for(String name : formatNames) {
                    Node root = metadata.getAsTree(name);
                    parseNode(1, root);
                }
            }
            System.out.println("Extra Metadata Format Names");
            formatNames = metadata.getExtraMetadataFormatNames();
            if(formatNames != null) {
                for(String name : formatNames) {
                    Node root = metadata.getAsTree(name);
                    parseNode(1, root);
                }
            }
        }

        BufferedImage bi = reader.read(0, param);
        stream.close();
        reader.dispose();
        return bi;
    }

    /**
     * Implements opening a file.
     */
    private void open() {
        if(displayPanel == null) return;
        JFileChooser chooser = new JFileChooser();
        if(currentDir != null) {
            File file = new File(currentDir);
            if(file != null && file.exists()) {
                chooser.setCurrentDirectory(file);
            }
        }
        int result = chooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            Cursor oldCursor = getCursor();
            try {
                file = chooser.getSelectedFile();
                // Save the selected path for next time
                currentDir = chooser.getSelectedFile().getParentFile()
                    .getPath();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                BufferedImage image = openFile(file);
                imagePanel.setImage(image);
                this.setTitle(file.getName());
            } finally {
                setCursor(oldCursor);
            }
        }
    }

    /**
     * Saves the display frame to a file.
     */
    public void saveAs() {
        if(displayPanel == null) return;
        BufferedImage image = imagePanel.getImage();
        if(image == null) {
            Utils.errMsg("No image");
            return;
        }

        // Get the file name
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(currentDir));
        int result = chooser.showSaveDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return;

        // Process the file
        String fileName = chooser.getSelectedFile().getPath();
        File newFile = new File(fileName);
        if(newFile.exists()) {
            int selection = JOptionPane.showConfirmDialog(null,
                "File already exists:" + LS + fileName + "\nOK to replace?",
                "Warning", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if(selection != JOptionPane.OK_OPTION) return;
        }

        Cursor oldCursor = getCursor();
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if(readMode == ReadMode.JAI) {
                String type = "";
                String ext = Utils.getExtension(newFile);
                if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg")) {
                    type = "JPEG";
                } else if(ext.equalsIgnoreCase("png")) {
                    type = "PNG";
                } else if(ext.equalsIgnoreCase("gif")) {
                    type = "GIF";
                } else if(ext.equalsIgnoreCase("tiff")
                    || ext.equalsIgnoreCase("tif")) {
                    type = "TIFF";
                } else if(ext.equalsIgnoreCase("bmp")) {
                    type = "BMP";
                } else {
                    Utils.errMsg("Unsupported format: " + ext);
                    return;
                }
                try {
                    selectWriteMode();
                    BufferedImage newImage;
                    switch(writeMode) {
                    case IMAGEIO:
                        newImage = image;
                        ImageUtils.saveImage(image, newFile);
                        break;
                    case JAI_NOCONVERT:
                        newImage = image;
                        JAI.create("FileStore", newImage, newFile.getPath(),
                            type);
                        break;
                    case JAI_NOASSIGN:
                        newImage = convertToProfile(image, ICC_FILENAME, false);
                        JAI.create("FileStore", newImage, newFile.getPath(),
                            type);
                        break;
                    case JAI_ASSIGN:
                        newImage = convertToProfile(image, ICC_FILENAME, true);
                        JAI.create("FileStore", newImage, newFile.getPath(),
                            type);
                        break;
                    }
                } catch(Exception ex) {
                    Utils.excMsg("Save failed", ex);
                }
            } else {
                ImageUtils.saveImageToFile(image, currentDir);
            }
            if(newFile != null && newFile.exists()) {
                // Set the currentDirectory based on what was saved
                File parent = newFile.getParentFile();
                if(parent != null && parent.exists()) {
                    currentDir = parent.getPath();
                } else {
                    currentDir = newFile.getPath();
                }
            }
        } finally {
            setCursor(oldCursor);
        }
    }

    /**
     * Selects the read mode with a confirm dialog.
     */
    private void selectReadMode() {
        Object[] possibilities = {ReadMode.IMAGEIO, ReadMode.SPECIAL,
            ReadMode.SPECIAL_METADATA, ReadMode.JAI};
        ReadMode res = (ReadMode)JOptionPane.showInputDialog(this,
            "Select a read mode", "Read Mode", JOptionPane.QUESTION_MESSAGE,
            null, possibilities, readMode);

        // If a string was returned, say so.
        if(res != null) {
            readMode = res;
        }
    }

    /**
     * Selects the write mode with a confirm dialog.
     */
    private void selectWriteMode() {
        Object[] possibilities = {WriteMode.IMAGEIO, WriteMode.JAI_NOCONVERT,
            WriteMode.JAI_NOASSIGN, WriteMode.JAI_ASSIGN};
        WriteMode res = (WriteMode)JOptionPane.showInputDialog(this,
            "Select a write mode", "Write Mode", JOptionPane.QUESTION_MESSAGE,
            null, possibilities, writeMode);

        // If a string was returned, say so.
        if(res != null) {
            writeMode = res;
        }
    }

    public static void listRegistry() {
        System.out.println("JAI Build Version=" + JAI.getBuildVersion());
        OperationRegistry or = JAI.getDefaultInstance().getOperationRegistry();
        String[] modeNames = RegistryMode.getModeNames();
        String[] descriptorNames;

        // System.out.println("OperationDescriptor Names:");
        // String[] descriptorNames1 =
        // or.getDescriptorNames(OperationDescriptor.class);
        // for(String string : descriptorNames1) {
        // System.out.println("  " + string);
        // }

        for(int i = 0; i < modeNames.length; i++) {
            System.out.println("For registry mode: " + modeNames[i]);

            descriptorNames = or.getDescriptorNames(modeNames[i]);
            for(int j = 0; j < descriptorNames.length; j++) {
                System.out.print("  Registered Operator: ");
                System.out.println(descriptorNames[j]);
            }
        }
    }

    // public static void convertProfile(BufferedImage image) {
    // ColorModel cm = image.getColorModel();
    // WritableRaster wr = image.getRaster();
    // boolean isAlphaPremultiplied = image.isAlphaPremultiplied();
    //
    // int pixelBits;
    // int[] bits;
    // boolean hasAlpha = cm.hasAlpha();
    // int transparency = cm.getTransparency();
    // int transferType = cm.getTransferType();
    //
    // ColorModel cs = new ColorSpace();
    //
    //
    //
    // ColorModel newCm = new ColorModel(pixelBits, bits, hasAlpha,
    // isAlphaPremultiplied, transparency, transferType, );
    //
    // BufferedImage newImage = new BufferedImage(cm, wr, isAlphaPremultiplied,
    // null);
    //
    // }

    /**
     * Converts an image to the profile contained in the file with the given
     * file name.
     * 
     * @param image
     * @param iccFileName
     * @param assign True to not change the data, just the profile.
     * @return
     */
    public static BufferedImage convertToProfile(BufferedImage image,
        String iccFileName, boolean assign) {
        // Convert to the profile
        ICC_Profile profile = null;
        try {
            profile = ICC_Profile.getInstance(iccFileName);
        } catch(Exception ex) {
            Utils.excMsg("Cannot open profile file", ex);
            return null;
        }
        Raster data = image.getData();
        image = ImageUtils.convertProfile(profile, image);
        ICC_ColorSpace cs = new ICC_ColorSpace(profile);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        BufferedImage newImage = op.filter(image, null);
        if(assign) {
            newImage.setData(data);
        }

        return newImage;
    }

    class ImagePanel extends JPanel
    {
        private static final long serialVersionUID = 1L;
        private BufferedImage image;

        public ImagePanel() {
            // Set up mouse listeners
            mouseAdapter = new MouseInputAdapter() {
                public void mouseMoved(MouseEvent ev) {
                    if(imagePanel.getImage() != null) {
                        int x = (int)(ev.getX());
                        int y = (int)(ev.getY());
                        String text = "x=" + x + " y=" + y + " "
                            + imagePanel.getColorString(x, y);
                        updateStatus(text);
                    }
                }

                public void mouseExited(MouseEvent ev) {
                    updateStatus(" ");
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(image == null) {
                return;
            }
            g.drawImage(image, 0, 0, null);
        }

        public String getColorString(int x, int y) {
            if(image == null || x < 0 || x >= image.getWidth() || y < 0
                || y >= image.getHeight()) {
                return "";
            }
            int rgbColor = image.getRGB(x, y);
            Color color = new Color(rgbColor);
            return String.format("(%3d, %3d, %3d)", color.getRed(),
                color.getGreen(), color.getBlue());
        }

        /**
         * @return The value of image.
         */
        public BufferedImage getImage() {
            return image;
        }

        /**
         * @param image The new value for image.
         */
        public void setImage(BufferedImage image) {
            this.image = image;
            this.repaint();
            this.revalidate();
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Set window decorations
            JFrame.setDefaultLookAndFeelDecorated(true);

            // Set the native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Make the job run in the AWT thread
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TestICCProfiles app = new TestICCProfiles();
                    // Make it exit when the window manager close button is
                    // clicked
                    app.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    app.pack();
                    app.setVisible(true);
                    app.setLocationRelativeTo(null);
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

}
