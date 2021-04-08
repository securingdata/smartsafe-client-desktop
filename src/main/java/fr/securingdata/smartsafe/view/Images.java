package fr.securingdata.smartsafe.view;

import fr.securingdata.smartsafe.util.ResourcesManager;
import javafx.scene.image.Image;

public interface Images {
	Image FAVICON         = new Image(ResourcesManager.getResourceAsStream("icons/favicon.png"));
	Image SMARTSAFE       = new Image(ResourcesManager.getResourceAsStream("icons/smart_safe.png"));
	Image ABOUT           = new Image(ResourcesManager.getResourceAsStream("icons/about.png"));
	Image ACCEPT          = new Image(ResourcesManager.getResourceAsStream("icons/accept.png"));
	Image BACKUP          = new Image(ResourcesManager.getResourceAsStream("icons/backup.png"));
	Image CONNECT         = new Image(ResourcesManager.getResourceAsStream("icons/connect.png"));
	Image COPY_PASS       = new Image(ResourcesManager.getResourceAsStream("icons/copy_pass.png"));
	Image COPY            = new Image(ResourcesManager.getResourceAsStream("icons/copy.png"));
	Image DELETE          = new Image(ResourcesManager.getResourceAsStream("icons/delete.png"));
	Image DETAILS         = new Image(ResourcesManager.getResourceAsStream("icons/details.png"));
	Image DISCONNECT      = new Image(ResourcesManager.getResourceAsStream("icons/disconnect.png"));
	Image EDIT            = new Image(ResourcesManager.getResourceAsStream("icons/edit.png"));
	Image ERROR           = new Image(ResourcesManager.getResourceAsStream("icons/error.png"));
	Image EXIT            = new Image(ResourcesManager.getResourceAsStream("icons/exit.png"));
	Image GOTO            = new Image(ResourcesManager.getResourceAsStream("icons/goto.png"));
	Image HELP            = new Image(ResourcesManager.getResourceAsStream("icons/help.png"));
	Image MOVE_DOWN       = new Image(ResourcesManager.getResourceAsStream("icons/move_down.png"));
	Image MOVE_TO         = new Image(ResourcesManager.getResourceAsStream("icons/move_to.png"));
	Image MOVE_UP         = new Image(ResourcesManager.getResourceAsStream("icons/move_up.png"));
	Image NEW_ENTRY       = new Image(ResourcesManager.getResourceAsStream("icons/new_entry.png"));
	Image NEW_GROUP       = new Image(ResourcesManager.getResourceAsStream("icons/new_group.png"));
	Image PIN             = new Image(ResourcesManager.getResourceAsStream("icons/pin.png"));
	Image PREFERENCES     = new Image(ResourcesManager.getResourceAsStream("icons/preferences.png"));
	Image PROPERTIES      = new Image(ResourcesManager.getResourceAsStream("icons/properties.png"));
	Image RENAME          = new Image(ResourcesManager.getResourceAsStream("icons/rename.png"));
	Image SHOW_PASS       = new Image(ResourcesManager.getResourceAsStream("icons/show_pass.png"));
	Image UPDATE          = new Image(ResourcesManager.getResourceAsStream("icons/update.png"));
	Image WARNING         = new Image(ResourcesManager.getResourceAsStream("icons/warning.png"));
}
