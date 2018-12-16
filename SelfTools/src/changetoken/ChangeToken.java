package changetoken;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;


public class ChangeToken {
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private static int width = 400;
	private static int height = 300;;
	private static int widthLoc = (screenSize.width-width)/2;
	private static int heightLoc = (screenSize.height-height)/2;
	private static int mode = 0;//1表示将英文标点替换成中文标点,0反之
	private String fileFullPath ;
	private Map<Character,Character> map = new HashMap<Character,Character>();
	public ChangeToken(String fileFullPath) {
		this.fileFullPath = fileFullPath;
	}
	public ChangeToken(String fileFullPath,int mode) {
		this.fileFullPath = fileFullPath;
		this.mode = mode;
	}
	
	public void change(){
		initMap();
		String start = fileFullPath.substring(0, fileFullPath.lastIndexOf('.'));
		String end = fileFullPath.substring(fileFullPath.lastIndexOf('.'));
//		File file = new File(start+"1"+end);
		Reader reader = null;
		Writer writer =null;
		try {
			reader = new InputStreamReader(new FileInputStream(fileFullPath),"utf-8");
			writer = new OutputStreamWriter(new FileOutputStream(start+"1"+end),"utf-8");
			int i=-1;
			while((i = reader.read())>=0){
				if(map.keySet().contains((char)i)){
					//System.out.println(i);
					i = map.get((char)i);
				}
				writer.write(i);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("未知错误!");
		}finally{
			try {
				reader.close();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void initMap() {
		map.put('（', '(');
		map.put('）', ')');
		map.put('；', ';');
		map.put('＜', '<');
		map.put('＞', '>');
		map.put('｝', '}');
		map.put('｛', '{');
		map.put('．', '.');
		map.put('，', ',');
		map.put('＋', '+');
		map.put('－', '-');
		map.put('！', '!');
	}
	
	public static void main(String[] args) {
		JFrame jFrame = new JFrame("changeToken");
		jFrame.setLayout(new BorderLayout());
		JTextField field = new JTextField("C:\\Users\\hgy\\Desktop\\1.txt");
		JButton jButton = new JButton("start");
		
		jButton.addMouseListener(new MouseListener2(field));
		
		jFrame.add(field,BorderLayout.NORTH);
		jFrame.add(jButton,BorderLayout.CENTER);
		
		
		jFrame.setLocation(widthLoc,heightLoc);
		jFrame.setSize(width, height);
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setVisible(true);
	}
}
/**
 * 继承适配器可以减少不必要的方法
 * 实现接口可以提供多一次的继承机会
 * @author hgy
 */
class MouseListener2 extends MouseAdapter{
	String Text = "";
	JTextField jtf = null;

	public MouseListener2(JTextField field) {
		jtf = field;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		new ChangeToken(jtf.getText()).change();
	}
}