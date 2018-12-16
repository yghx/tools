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

	// 初始化窗口
	private void init() {
		// 设置风格
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			System.out.println("设置风格失败");
		}
		// 布局
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
		// 监听
		select.addActionListener(new SelectFileListener(textArea));
		start.addActionListener(new ExecuteChangeCharset(textArea, box));

		this.setLocation(widthLoc, heightLoc);
		this.setSize(width, height);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}

	// 执行转换
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
				myFile.changeCharset(box.getSelectedItem().toString());// 设置目标字符集
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

	// 选择文件
	class SelectFileListener implements ActionListener {
		private JTextArea textArea;

		public SelectFileListener(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			// // 获取桌面路径
			// File desktopDir =
			// FileSystemView.getFileSystemView().getHomeDirectory();
			// String desktopPath = desktopDir.getAbsolutePath();
			File latestFile = new File(ChangeCharset.latestLoc);
			fileChooser.setCurrentDirectory(latestFile);// 默认打开Document
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);// 设置可选择文件或目录
			int rVal = fileChooser.showOpenDialog(ChangeCharset.this);
			if (rVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				ChangeCharset.latestLoc = fileChooser.getCurrentDirectory().getAbsolutePath();// 维护最近位置
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

	// 组合模式处理目录和文件
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
		 * 该方法完全不靠谱,.xml.java等文件都不能正确判断
		 * 
		 * @param f
		 * @return
		 */
		private boolean isTxt(File f) {
			String type = new MimetypesFileTypeMap().getContentType(f);
			System.out.println("type=" + type);
			// text/html html
			// text/plain txt
			// application/octet-stream png/jar/link 表示二进制文件
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
		 * 版本v1.3 IO
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
						sb.append(new String(byteBuffer.array(), 0, i, sourceCharset));// 不指定字符集就使用默认字符集iso-8859-1
					}
					// public FileOutputStream(String name, boolean
					// append),可以使用追加方式
					out = new FileOutputStream(f).getChannel();// 创建out时候要么新建要么会清空原有文件

					ByteBuffer wrap = ByteBuffer.wrap((sb.toString().getBytes(charset)));
					out.write(wrap);// 转码
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
		 * 版本v1.2 IO 要保证转码乱码关键是获取String不是乱码的 String是平台无关的
		 * 如果使用字节流就必须使用对应字符集对bytes编码,否则乱码 如果使用字符流就可以避免这个问题
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
					// iso-8859-1并不是万能码,万能码是String(Unicode码),只是因为web应用中默认使用iso-8859-1
					// iso-8859-1并不能解码成任意字符集,还是依赖于String
					// 下面一行不管是转化成原来编码还是新编码,只要不是iso-8859-1兼容编码都将乱码
					// new
					// String(bytes_iso-8859-1,charset),正确转化成任意字符集String是个错误的结论,iso-8859-1不是万能转化的字符集
					// String st = new
					// String(sb.toString().getBytes("iso-8859-1"), "gbk");
					out.write(sb.toString().getBytes(charset));// 必须指定使用目标编码
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
		 * 版本v1.1 RandmoAccessFile 清空不便
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
					raf.write(sb.toString().getBytes(charset));// 转码
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
		 * 版本v1.0,清空不便
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
					inputStreamReader = new InputStreamReader(new FileInputStream(f), sourceCharset);// 读取必须保持和编码一致
					br = new BufferedReader(inputStreamReader);
					String s = null;
					while ((s = br.readLine()) != null) {
						sb.append(s).append("\r\n");
					}
					// AccessFile不适合需要清空的情况
					raf.write(sb.toString().getBytes(charset));// 转码
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
	 * 不能解决没有头部BOM情况
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
	 * 不能解决没有头部BOM情况
	 */
	public static String resolveCode(File file) throws IOException {
		try (InputStream inputStream = new FileInputStream(file)) {
			byte[] head = new byte[3];
			inputStream.read(head);
			String code = "gb2312"; // 或GBK
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
