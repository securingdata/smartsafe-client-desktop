package util;

import java.security.Permission;

public class CustomSecurityManager extends SecurityManager {
	private static final String EXIT_PERM_NAME = "exitVM.";
	private static final CustomSecurityManager csm = new CustomSecurityManager();
	
	private boolean blockExit;
	
	private CustomSecurityManager() {
		blockExit = false;
	}
	
	public void checkPermission(Permission perm) {
        if (perm.getName().startsWith(EXIT_PERM_NAME) && blockExit) {
        	throw new ExitException(perm.getName().replaceAll(EXIT_PERM_NAME, ""));
        }
    }
	
	public boolean isExitBlocked() {
		return blockExit;
	}
	public void setBlockExit(boolean b) {
		blockExit = b;
	}
	
	public static CustomSecurityManager getSecurityManager() {
		return csm;
	}
}
