package br.ufsc.egc.rudger.expertprofiling;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import br.ufsc.egc.rudger.expertprofiling.uima.Slf4jLoggerImpl;

public class ExpertProfilingGuiApp extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final File historyFile;

    static {
        historyFile = ExpertProfilingPathUtil.getPath("gui-hystory");
    }

    JButton btRun;
    JTextArea taLog;
    JFileChooser fcFolder;
    private JTextField tfFolder;
    private JTextField tfUserName;

    public ExpertProfilingGuiApp() {
        this.setBorder(new EmptyBorder(20, 20, 20, 20));

        this.fcFolder = new JFileChooser(this.setHistoryPath());
        this.fcFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {410, 0};
        gridBagLayout.rowHeights = new int[] {0, 94, 102, 0};
        gridBagLayout.columnWeights = new double[] {1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {0.0, 0.0, 1.0, Double.MIN_VALUE};
        this.setLayout(gridBagLayout);

        JPanel pnOrientation = new JPanel();
        pnOrientation.setForeground(new Color(0, 0, 0));
        pnOrientation.setBorder(new CompoundBorder(
                new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Information", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)),
                new EmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc_pnOrientation = new GridBagConstraints();
        gbc_pnOrientation.insets = new Insets(0, 0, 5, 0);
        gbc_pnOrientation.fill = GridBagConstraints.BOTH;
        gbc_pnOrientation.gridx = 0;
        gbc_pnOrientation.gridy = 0;
        this.add(pnOrientation, gbc_pnOrientation);
        GridBagLayout gbl_pnOrientation = new GridBagLayout();
        gbl_pnOrientation.columnWidths = new int[] {203, 0};
        gbl_pnOrientation.rowHeights = new int[] {0, 0};
        gbl_pnOrientation.columnWeights = new double[] {1.0, Double.MIN_VALUE};
        gbl_pnOrientation.rowWeights = new double[] {0.0, Double.MIN_VALUE};
        pnOrientation.setLayout(gbl_pnOrientation);

        JTextArea txtrValor = new JTextArea();
        txtrValor.setWrapStyleWord(true);
        txtrValor.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtrValor.setBackground(UIManager.getColor("Button.background"));
        txtrValor.setEditable(false);
        txtrValor.setLineWrap(true);
        txtrValor.setText(
                "This project was conceived in order to extract information about knowledge and interests of experts from unstructured documents and in natural language. To accomplish this extraction, the documents contained in an expert’s folder are analyzed. The contents extracted of these documents generates tag cloud, profile timeline and a report per year of the main concepts found. This application uses the dbpedia database in english and portuguese languages. When you run it in the first time, the application will download this content and it can take a few minutes. Be patient, please. The procedures will be presented in text box below.");
        GridBagConstraints gbc_txtrValor = new GridBagConstraints();
        gbc_txtrValor.fill = GridBagConstraints.BOTH;
        gbc_txtrValor.gridx = 0;
        gbc_txtrValor.gridy = 0;
        pnOrientation.add(txtrValor, gbc_txtrValor);

        JPanel pnConfiguration = new JPanel();
        pnConfiguration.setBorder(new CompoundBorder(
                new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Configuration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)),
                new EmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc_pnConfiguration = new GridBagConstraints();
        gbc_pnConfiguration.anchor = GridBagConstraints.NORTH;
        gbc_pnConfiguration.fill = GridBagConstraints.HORIZONTAL;
        gbc_pnConfiguration.insets = new Insets(0, 0, 5, 0);
        gbc_pnConfiguration.gridx = 0;
        gbc_pnConfiguration.gridy = 1;
        this.add(pnConfiguration, gbc_pnConfiguration);
        GridBagLayout gbl_pnConfiguration = new GridBagLayout();
        gbl_pnConfiguration.columnWidths = new int[] {105, 0, 15, 0};
        gbl_pnConfiguration.rowHeights = new int[] {20, 21, 0};
        gbl_pnConfiguration.columnWeights = new double[] {0.0, 1.0, 0.0, 0.0};
        gbl_pnConfiguration.rowWeights = new double[] {0.0, 0.0, Double.MIN_VALUE};
        pnConfiguration.setLayout(gbl_pnConfiguration);

        JLabel lblFolder = new JLabel("Folder:");
        lblFolder.setFont(new Font("Tahoma", Font.BOLD, 11));
        GridBagConstraints gbc_lblFolder = new GridBagConstraints();
        gbc_lblFolder.anchor = GridBagConstraints.EAST;
        gbc_lblFolder.fill = GridBagConstraints.VERTICAL;
        gbc_lblFolder.insets = new Insets(0, 0, 5, 5);
        gbc_lblFolder.gridx = 0;
        gbc_lblFolder.gridy = 0;
        pnConfiguration.add(lblFolder, gbc_lblFolder);
        this.btRun = new JButton("Select folder...");
        this.btRun.addActionListener(this);

        this.tfFolder = new JTextField();
        this.tfFolder.setEditable(false);
        GridBagConstraints gbc_tfPath = new GridBagConstraints();
        gbc_tfPath.fill = GridBagConstraints.BOTH;
        gbc_tfPath.insets = new Insets(0, 0, 5, 5);
        gbc_tfPath.gridx = 1;
        gbc_tfPath.gridy = 0;
        pnConfiguration.add(this.tfFolder, gbc_tfPath);
        this.tfFolder.setColumns(3);
        GridBagConstraints gbc_openButton = new GridBagConstraints();
        gbc_openButton.insets = new Insets(0, 0, 5, 0);
        gbc_openButton.fill = GridBagConstraints.BOTH;
        gbc_openButton.gridx = 3;
        gbc_openButton.gridy = 0;
        pnConfiguration.add(this.btRun, gbc_openButton);

        JLabel lblName = new JLabel("Expert name:");
        lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
        GridBagConstraints gbc_lblName = new GridBagConstraints();
        gbc_lblName.anchor = GridBagConstraints.EAST;
        gbc_lblName.insets = new Insets(0, 0, 0, 5);
        gbc_lblName.gridx = 0;
        gbc_lblName.gridy = 1;
        pnConfiguration.add(lblName, gbc_lblName);

        this.tfUserName = new JTextField();
        GridBagConstraints gbc_tfUserName = new GridBagConstraints();
        gbc_tfUserName.insets = new Insets(0, 0, 0, 5);
        gbc_tfUserName.fill = GridBagConstraints.BOTH;
        gbc_tfUserName.gridx = 1;
        gbc_tfUserName.gridy = 1;
        pnConfiguration.add(this.tfUserName, gbc_tfUserName);
        this.tfUserName.setColumns(10);

        JButton btnRun = new JButton("Run");
        btnRun.addActionListener(e -> {
            this.run();
        });
        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnRun.gridx = 3;
        gbc_btnRun.gridy = 1;
        pnConfiguration.add(btnRun, gbc_btnRun);

        this.taLog = new JTextArea(5, 20);
        this.taLog.setMargin(new Insets(5, 5, 5, 5));
        this.taLog.setEditable(false);
        JScrollPane spnLogging = new JScrollPane(this.taLog);
        GridBagConstraints gbc_spnLogging = new GridBagConstraints();
        gbc_spnLogging.fill = GridBagConstraints.BOTH;
        gbc_spnLogging.gridx = 0;
        gbc_spnLogging.gridy = 2;
        this.add(spnLogging, gbc_spnLogging);

        PrintStream out = new PrintStream(new OutputStream() {

            @Override
            public void write(final int b) throws IOException {
                ExpertProfilingGuiApp.this.taLog.append(String.valueOf((char) b));
                ExpertProfilingGuiApp.this.taLog.setCaretPosition(ExpertProfilingGuiApp.this.taLog.getDocument().getLength());
            }

        });
        System.setOut(out);
        System.setErr(out);
    }

    private File setHistoryPath() {
        try {
            return new File(FileUtils.readFileToString(historyFile));
        } catch (Exception e) {
            // do nothing...
        }

        return new File(System.getProperty("user.home"));
    }

    private void setHistoryPath(final File path) {
        try {
            FileUtils.write(historyFile, path.getAbsolutePath());
        } catch (IOException e) {
            // do nothing...
        }
    }

    private void run() {
        if (StringUtils.isBlank(this.tfUserName.getText()) || StringUtils.isBlank(this.tfFolder.getText())) {
            JOptionPane.showMessageDialog(null, "Please, to run this program you need to fill in the folder and the user name fields.", "Empty fields",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            this.taLog.setText("");

            ExpertProfilingPipeline.Configuration config = this.createConfig();
            new Thread(() -> {
                try {
                    ExpertProfilingPipeline epp = new ExpertProfilingPipeline();
                    Desktop.getDesktop().browse(epp.run(config).toURI());
                } catch (Exception e) {
                    StringBuilder sb = new StringBuilder(e.toString());
                    for (StackTraceElement ste : e.getStackTrace()) {
                        sb.append("\n\tat ");
                        sb.append(ste);
                    }

                    JOptionPane.showMessageDialog(null, sb.toString() + "\nTry to delete the application folder and run it again.", "Unexpected error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }).start();
        }
    }

    private ExpertProfilingPipeline.Configuration createConfig() {
        ExpertProfilingPipeline.Configuration config = new ExpertProfilingPipeline.Configuration();

        List<String> dppediaFiles = new ArrayList<>();
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/pt/skos_categories_pt.ttl.bz2");
        dppediaFiles.add("http://downloads.dbpedia.org/2015-10/core-i18n/en/skos_categories_en.ttl.bz2");
        config.setDppediaFiles(dppediaFiles);

        List<String> extensions = new ArrayList<>();
        extensions.add("**/*.pdf");
        extensions.add("**/*.txt");
        extensions.add("**/*.docx");
        extensions.add("**/*.doc");
        extensions.add("**/*.ppt");
        extensions.add("**/*.pptx");
        config.setExtensions(extensions);

        List<String> stopwordFiles = new ArrayList<>();
        stopwordFiles.add("stopwords/stopwords_pt_BR.txt");
        stopwordFiles.add("stopwords/stopwords_en.txt");
        config.setStopWordFiles(stopwordFiles);

        config.setUserName(this.tfUserName.getText());
        config.setUserCode(this.tfUserName.getText());
        config.setSourceLocation(this.tfFolder.getText());
        return config;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        int returnVal = this.fcFolder.showOpenDialog(ExpertProfilingGuiApp.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = this.fcFolder.getSelectedFile();
            this.tfFolder.setText(file.getAbsolutePath());
            this.setHistoryPath(file);
        }

    }

    private static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Expert Profiling - Simple Desktop Application");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add content to the window.
        frame.getContentPane().add(new ExpertProfilingGuiApp());

        // Display the window.
        frame.setVisible(true);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
    }

    public static void main(final String[] args) {
        Slf4jLoggerImpl.forceUsingThisImplementation();

        SwingUtilities.invokeLater(() -> {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            createAndShowGUI();
        });
    }

}