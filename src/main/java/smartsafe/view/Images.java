package smartsafe.view;

import util.ResourcesManager;

import javafx.scene.image.Image;

public interface Images {
	Image SMARTSAFE       = new Image(ResourcesManager.getResourceAsStream("icons/smart_safe.png"));
	Image ABOUT           = new Image(ResourcesManager.getResourceAsStream("icons/about.gif"));
	Image BACKUP          = new Image(ResourcesManager.getResourceAsStream("icons/backup.gif"));
	Image CONNECT         = new Image(ResourcesManager.getResourceAsStream("icons/connect.png"));
	Image COPY_PASS       = new Image(ResourcesManager.getResourceAsStream("icons/copy_pass.gif"));
	Image COPY            = new Image(ResourcesManager.getResourceAsStream("icons/copy.gif"));
	Image DELETE          = new Image(ResourcesManager.getResourceAsStream("icons/delete.gif"));
	Image DETAILS         = new Image(ResourcesManager.getResourceAsStream("icons/details.gif"));
	Image DISCONNECT      = new Image(ResourcesManager.getResourceAsStream("icons/disconnect.gif"));
	Image EDIT            = new Image(ResourcesManager.getResourceAsStream("icons/edit.gif"));
	Image GOTO            = new Image(ResourcesManager.getResourceAsStream("icons/goto.gif"));
	Image HELP            = new Image(ResourcesManager.getResourceAsStream("icons/help.png"));
	Image NEW_ENTRY       = new Image(ResourcesManager.getResourceAsStream("icons/new_entry.png"));
	Image NEW_GROUP       = new Image(ResourcesManager.getResourceAsStream("icons/new_group.png"));
	Image NEW             = new Image(ResourcesManager.getResourceAsStream("icons/new.gif"));
	Image PIN             = new Image(ResourcesManager.getResourceAsStream("icons/pin.gif"));
	Image PREFERENCES     = new Image(ResourcesManager.getResourceAsStream("icons/preferences.gif"));
	Image PROPERTIES      = new Image(ResourcesManager.getResourceAsStream("icons/properties.gif"));
	Image SHOW_PASS       = new Image(ResourcesManager.getResourceAsStream("icons/show_pass.gif"));
	Image UPDATE          = new Image(ResourcesManager.getResourceAsStream("icons/update.gif"));
	Image INIT            = new Image(ResourcesManager.getResourceAsStream("icons/init.gif"));
	Image WARNING         = new Image(ResourcesManager.getResourceAsStream("icons/warning.gif"));
}
