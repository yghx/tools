package eclipsestarttime;

import jdk.nashorn.tools.Shell;

/**
 * ͳ��Eclipse������ʱ
 * 
 * @author zzm
 */
public class ShowTime implements IStartup {
	public void earlyStartup() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				long eclipseStartTime = Long.parseLong(System.getProperty("eclipse.startTime"));
				long costTime = System.currentTimeMillis() - eclipseStartTime;
				Shell shell = Display.getDefault().getActiveShell();
				String message = "Eclipse������ʱ��" + costTime + "ms";
				MessageDialog.openInformation(shell, "Information", message);
			}
		});
	}
}