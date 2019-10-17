package smartsafe.view;

import util.ResourcesManager;

import javafx.scene.image.Image;

public interface Images {
	Image CONNECT         = new Image(ResourcesManager.getResourceAsStream("icons/connect.png"));
	Image DISCONNECT      = new Image(ResourcesManager.getResourceAsStream("icons/disconnect.gif"));
	Image NEW             = new Image(ResourcesManager.getResourceAsStream("icons/new.gif"));
	Image NEW_GROUP       = new Image(ResourcesManager.getResourceAsStream("icons/new_group.png"));
	Image NEW_ENTRY       = new Image(ResourcesManager.getResourceAsStream("icons/new_entry.png"));
	Image DELETE          = new Image(ResourcesManager.getResourceAsStream("icons/delete.gif"));
	Image EDIT            = new Image(ResourcesManager.getResourceAsStream("icons/edit.gif"));
	Image GOTO            = new Image(ResourcesManager.getResourceAsStream("icons/goto.gif"));
	Image COPY            = new Image(ResourcesManager.getResourceAsStream("icons/copy.gif"));
	Image DETAILS         = new Image(ResourcesManager.getResourceAsStream("icons/details.gif"));
	Image HELP            = new Image(ResourcesManager.getResourceAsStream("icons/help.png"));
	Image ABOUT           = new Image(ResourcesManager.getResourceAsStream("icons/about.gif"));
}
