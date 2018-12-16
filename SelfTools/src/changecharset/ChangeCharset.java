package changecharset;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

public class ChangeCharset extends JFrame {
	private static final long serialVersionUID = 1L;
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private static int width = 400;
	private static int height = 300;;
	private static int widthLoc = (screenSize.width - width) / 2;
	private static int heightLoc = (screenSize.height - height) / 2;
	private static String latestLoc = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
	private String[] charsets = { "utf-8", "gbk", "iso-8859-1", "gb2312", "utf-16" };

	public ChangeCharset() {
		init();
	}

	// ��ʼ������
	private void init() {
		// ���÷��
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			System.out.println("���÷��ʧ��");
		}
		// ����
		JPanel panel = new JPanel();
		GridLayout gLayout = new GridLayout(1, 2);
		panel.setLayout(gLayout);
		JButton select = new JButton("select file or directory");
		JComboBox<String> box = new JComboBox<>();
		for (String charset : charsets) {
			box.addItem(charset);
		}
		panel.add(select);
		panel.add(box);
		this.setLayout(new BorderLayout());
		JTextArea textArea = new JTextArea("you do not select any file!please click top button!");
		textArea.setEnabled(false);
		JButton start = new JButton("start switch");

		this.add(panel, BorderLayout.NORTH);
		this.add(textArea, BorderLayout.CENTER);
		this.add(start, BorderLayout.SOUTH);
		// ����
		select.addActionListener(new SelectFileListener(textArea));
		start.addActionListener(new ExecuteChangeCharset(textArea, box));

		this.setLocation(widthLoc, heightLoc);
		this.setSize(width, height);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	// ִ��ת��
	class ExecuteChangeCharset implements ActionListener {
		private JTextArea textArea;
		private JComboBox<String> box;

		public ExecuteChangeCharset(JTextArea textArea, JComboBox<String> box) {
			this.textArea = textArea;
			this.box = box;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String path = textArea.getText();
			MyFile myFile = new MyFile(path);
			System.out.println("text  = " + path);
			long start = System.currentTimeMillis();
			System.out.println("start : " + new Date());
			try {
				myFile.changeCharset(box.getSelectedItem().toString());// ����Ŀ���ַ���
			} catch (IOException e1) {
				e1.printStackTrace();
				System.out.println("IOEXception");
			}
			long end = System.currentTimeMillis();
			System.out.println("end : " + new Date());
			String times = "times : " + ((end - start) / 1000.0) + "s";
			System.out.println(times);
			textArea.setText(times);
		}

	}

	// ѡ���ļ�
	class SelectFileListener implements ActionListener {
		private JTextArea textArea;

		public SelectFileListener(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			// // ��ȡ����·��
			// File desktopDir =
			// FileSystemView.getFileSystemView().getHomeDirectory();
			// String desktopPath = desktopDir.getAbsolutePath();
			File latestFile = new File(ChangeCharset.latestLoc);
			fileChooser.setCurrentDirectory(latestFile);// Ĭ�ϴ�Document
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);// ���ÿ�ѡ���ļ���Ŀ¼
			int rVal = fileChooser.showOpenDialog(ChangeCharset.this);
			if (rVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				ChangeCharset.latestLoc = fileChooser.getCurrentDirectory().getAbsolutePath();// ά�����λ��
				if (selectedFile != null) {
					textArea.setText(selectedFile.getAbsolutePath());
					return;
				}
				File currentDirectory = fileChooser.getCurrentDirectory();
				if (currentDirectory != null)
					textArea.setText(currentDirectory.getAbsolutePath());
			}
			if (rVal == JFileChooser.CANCEL_OPTION)
				textArea.setText("you do not select any file!please click top!");
		}

	}

	// ���ģʽ����Ŀ¼���ļ�
	class MyFile extends File {
		private static final long serialVersionUID = 1L;

		public MyFile(String pathname) {
			super(pathname);
		}

		public void changeCharset(String charset) throws IOException {
			// dealFile(this,charset);//error
			// dealFileRandomAccessFile(this, charset);//error
			// dealFileIO(this, charset);
			dealFileNIO(this, charset);
		}

		/**
		 * �÷�����ȫ������,.xml.java���ļ���������ȷ�ж�
		 * 
		 * @param f
		 * @return
		 */
		private boolean isTxt(File f) {
			String type = new MimetypesFileTypeMap().getContentType(f);
			System.out.println("type=" + type);
			// text/html html
			// text/plain txt
			// application/octet-stream png/jar/link ��ʾ�������ļ�
			return true;

		}

		@SuppressWarnings("static-access")
		private boolean isTxtByMagic(File f) {
			Magic parser = new Magic();
			MagicMatch match = null;
			try {
				match = parser.getMagicMatch(f, false);
			} catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
				e.printStackTrace();
			}
			System.out.println(match.getMimeType());
			return true;
		}

		/**
		 * �汾v1.3 IO
		 */
		@SuppressWarnings("resource")
		private void dealFileNIO(File f, String charset) throws IOException {
			isTxtByMagic(f);
			if (f.exists() && f.isFile()) {
				String readFileToString = FileUtils.readFileToString(f);
				if (readFileToString == null || "".equals(readFileToString)) {
					return;
				}
				String sourceCharset = EncodingDetect.getJavaEncode(f.getAbsolutePath());
				FileChannel in = null;
				FileChannel out = null;
				ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
				StringBuilder sb = new StringBuilder();
				try {
					in = new FileInputStream(f).getChannel();
					int i = -1;
					while ((i = in.read(byteBuffer)) != -1) {
						sb.append(new String(byteBuffer.array(), 0, i, sourceCharset));// ��ָ���ַ�����ʹ��Ĭ���ַ���iso-8859-1
					}
					// public FileOutputStream(String name, boolean
					// append),����ʹ��׷�ӷ�ʽ
					out = new FileOutputStream(f).getChannel();// ����outʱ��Ҫô�½�Ҫô�����ԭ���ļ�

					ByteBuffer wrap = ByteBuffer.wrap((sb.toString().getBytes(charset)));
					out.write(wrap);// ת��
				} finally {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				}
			}
			if (f.exists() && f.isDirectory()) {
				File[] listFiles = f.listFiles();
				for (File ff : listFiles) {
					dealFileNIO(ff, charset);
				}
			}
		}

		/**
		 * �汾v1.2 IO Ҫ��֤ת������ؼ��ǻ�ȡString��������� String��ƽ̨�޹ص�
		 * ���ʹ���ֽ����ͱ���ʹ�ö�Ӧ�ַ�����bytes����,�������� ���ʹ���ַ����Ϳ��Ա����������
		 */
		private void dealFileIO(File f, String charset) throws IOException {
			if (f.exists() && f.isFile()) {
				String readFileToString = FileUtils.readFileToString(f);
				if (readFileToString == null || "".equals(readFileToString)) {
					return;
				}
				String sourceCharset = EncodingDetect.getJavaEncode(f.getAbsolutePath());
				InputStream in = null;
				OutputStream out = null;
				StringBuilder sb = new StringBuilder();
				try {
					in = new FileInputStream(f);
					byte[] buf = new byte[1024];
					int i = -1;
					while ((i = in.read(buf)) != -1) {
						sb.append(new String(buf, 0, i, sourceCharset));
					}
					out = new FileOutputStream(f);
					// iso-8859-1������������,��������String(Unicode��),ֻ����ΪwebӦ����Ĭ��ʹ��iso-8859-1
					// iso-8859-1�����ܽ���������ַ���,����������String
					// ����һ�в�����ת����ԭ�����뻹���±���,ֻҪ����iso-8859-1���ݱ��붼������
					// new
					// String(bytes_iso-8859-1,charset),��ȷת���������ַ���String�Ǹ�����Ľ���,iso-8859-1��������ת�����ַ���
					// String st = new
					// String(sb.toString().getBytes("iso-8859-1"), "gbk");
					out.write(sb.toString().getBytes(charset));// ����ָ��ʹ��Ŀ�����
					out.flush();
				} finally {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				}
			}
			if (f.exists() && f.isDirectory()) {
				File[] listFiles = f.listFiles();
				for (File ff : listFiles) {
					dealFileIO(ff, charset);
				}
			}
		}

		/**
		 * �汾v1.1 RandmoAccessFile ��ղ���
		 */
		private void dealFileRandomAccessFile(File f, String charset) throws IOException {
			if (f.exists() && f.isFile()) {
				String readFileToString = FileUtils.readFileToString(f);
				if (readFileToString == null || "".equals(readFileToString)) {
					return;
				}
				String sourceCharset = EncodingDetect.getJavaEncode(f.getAbsolutePath());
				BufferedReader br = null;
				StringBuilder sb = new StringBuilder();
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				try {
					byte[] buf = new byte[1024];
					int i = -1;
					while ((i = raf.read(buf)) != -1) {
						sb.append(new String(buf, 0, i, sourceCharset));
					}
					raf.write(sb.toString().getBytes(charset));// ת��
				} finally {
					if (br != null)
						br.close();
					if (raf != null)
						raf.close();
				}
			}
			if (f.exists() && f.isDirectory()) {
				File[] listFiles = f.listFiles();
				for (File ff : listFiles) {
					dealFileRandomAccessFile(ff, charset);
				}
			}
		}

		/**
		 * �汾v1.0,��ղ���
		 */
		@SuppressWarnings("unused")
		private void dealFile(File f, String charset) throws IOException {
			if (f.exists() && f.isFile()) {
				String readFileToString = FileUtils.readFileToString(f);
				if (readFileToString == null || "".equals(readFileToString)) {
					return;
				}
				String sourceCharset = EncodingDetect.getJavaEncode(f.getAbsolutePath());
				InputStreamReader inputStreamReader = null;
				BufferedReader br = null;
				StringBuilder sb = new StringBuilder();
				RandomAccessFile raf = null;
				try {
					raf = new RandomAccessFile(f, "rw");
					inputStreamReader = new InputStreamReader(new FileInputStream(f), sourceCharset);// ��ȡ���뱣�ֺͱ���һ��
					br = new BufferedReader(inputStreamReader);
					String s = null;
					while ((s = br.readLine()) != null) {
						sb.append(s).append("\r\n");
					}
					// AccessFile���ʺ���Ҫ��յ����
					raf.write(sb.toString().getBytes(charset));// ת��
				} finally {
					if (br != null)
						br.close();
					if (raf != null)
						raf.close();
				}
			}
			if (f.exists() && f.isDirectory()) {
				File[] listFiles = f.listFiles();
				for (File ff : listFiles) {
					dealFile(ff, charset);
				}
			}
		}
	}

	/**
	 * ���ܽ��û��ͷ��BOM���
	 */
	public static String codeString(File file) throws IOException {
		String code = null;
		try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file))) {
			int p = (bin.read() << 8) + bin.read();
			bin.close();
			switch (p) {
			case 0xefbb:
				code = "UTF-8";
				break;
			case 0xfffe:
				code = "Unicode";
				break;
			case 0xfeff:
				code = "UTF-16BE";
				break;
			default:
				code = "GBK";
			}
		}

		return code;
	}

	/**
	 * ���ܽ��û��ͷ��BOM���
	 */
	public static String resolveCode(File file) throws IOException {
		try (InputStream inputStream = new FileInputStream(file)) {
			byte[] head = new byte[3];
			inputStream.read(head);
			String code = "gb2312"; // ��GBK
			if (head[0] == -1 && head[1] == -2)
				code = "UTF-16";
			else if (head[0] == -2 && head[1] == -1)
				code = "Unicode";
			else if (head[0] == -17 && head[1] == -69 && head[2] == -65)
				code = "UTF-8";

			inputStream.close();
			System.out.println(code);
			return code;
		}
	}

	public static void main(String[] args) {
		new ChangeCharset();
	}
}
