package delsurplusfiles;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DelFileUtil {
	static Map<String, File> files = new HashMap<String, File>();
	static Map<String, Object> msg = new HashMap<String, Object>();
	static int delNum = 0;

	public static Map<String, Object> delFile(String fileFullPath,String path) throws IOException {
		File file = new File(fileFullPath);

		if (file.exists()) {
			if (file.isDirectory()) {
				for (String fName : file.list()) {
					delFile(fileFullPath + File.separator + fName,fileFullPath);
				}
			} else if (file.isFile()) {
				String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
				String suffix = file.getName().substring(file.getName().lastIndexOf('.'));
				boolean b = false;
				if (fileName.indexOf('(') == -1) {
					b = false;
				} else {
					String temp = fileName.substring(fileName.indexOf('('));
					String reg = "^\\(\\d\\)$";
					b = Pattern.matches(reg, temp);
				}
				String simpleName = b ? fileName.substring(0, fileName.lastIndexOf('(')) : fileName;
				if (files.containsKey(simpleName)) {
					File oldFile = files.get(simpleName);
					if(b){
						// 判断文件大小后再删除
						if (oldFile.length() < file.length()) {
							// 删除旧文件
							oldFile.delete();
							files.put(simpleName, file);
							String newPath = path+File.separator+simpleName+suffix;
							File newFile = new File(newPath);
							if(newFile.exists()){
								newFile.delete();
							}
							file.renameTo(newFile);//newFile需要重新创建,数据给了newFile而没有给file
							files.put(simpleName, newFile);
						} else {
							file.delete();
						}
					}else{
						// 判断文件大小后再删除
						if (oldFile.length() < file.length()) {
							// 删除旧文件
							oldFile.delete();
							files.put(simpleName, file);
						} else if(!(oldFile.getPath()+oldFile.getName()).equals(file.getPath()+file.getName())){
							file.delete();
						}
					}
				} else {
					files.put(simpleName, file);
					if(b){
						String newPath = path+File.separator+simpleName+suffix;
						File newFile = new File(newPath);
						if(newFile.exists()){
							newFile.delete();
						}
						file.renameTo(newFile);//newFile需要重新创建,数据给了newFile而没有给file
						files.put(simpleName, newFile);
					}
				}
			} else {
				msg.put("msg", "文件有问题");
				msg.put("success", false);
			}
		} else {
			msg.put("msg", "文件不存在");
			msg.put("success", false);
		}
		return msg;
	}

	public static void main(String[] args) throws IOException {
		//delFile("I:\\kugou",null);
		delFile("I:\\kugou\\Songs",null);
//		delFile("C:\\Users\\hgy\\Desktop\\songs",null);
//		test();
	}
	public static void test(){
		File file0 = new File("C:\\Users\\hgy\\Desktop\\d","刘子菲.mp3");
		File file1 = new File("C:\\Users\\hgy\\Desktop\\d","刘子菲(1).mp3");
		File file2 = new File("C:\\Users\\hgy\\Desktop\\d","刘子菲(2).mp3");
		file0.delete();
		file1.renameTo(file0);
	}
}
