package smartsafe.view;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import compiler.CompilerException;
import connection.loader.GPCommands;
import connection.loader.GPException;
import connection.loader.SCP;
import connection.loader.SCP03;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.Project;
import smartsafe.comm.SmartSafeAppli;
import util.StringHex;

public class TestView extends Application {
	private static final StringHex PACK_AID = new StringHex("SmartSafe".getBytes());
	private static final StringHex APP_AID  = new StringHex("SmartSafeApp".getBytes());
	
	public static void main(String[] args) {
		/*build();
		reload();
		test();
		System.exit(0);*/
		
		launch(args);
	}
	
	public static void build() {
		Project p = new Project("SmartSafe", "..\\SmartSafeServer");
		p.parsePckgs();
		p.getPackages().get(0).setAid(PACK_AID);
		p.getPackages().get(0).setAppletsAID(Collections.singletonList(APP_AID));
		try {
			p.build();
		} catch (CompilerException e) {
			e.printStackTrace();
		}
	}
	public static void reload() {
		SCP scp = new SCP03();
		GPCommands gp = new GPCommands(scp);
		scp.setStaticDerivation(SCP.StaticDerivation.EMVCPS1_1);
		scp.addKey((short) 0, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
		scp.addKey((short) 1, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
		scp.addKey((short) 2, scp.instanciateKey(new StringHex("40 41 42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f").toBytes()));
		
		try {
			scp.coldReset();
			scp.select("");
			scp.initUpdate((byte) 0, (byte) 0);
			scp.externalAuth(SCP.SEC_LEVEL_C_MAC);
			
			String packAid = PACK_AID.toString();
			String appAid = APP_AID.toString();
			gp.delete(appAid, true);
			gp.delete(packAid, true);
			gp.installForLoad(packAid, "");
			gp.loadCAP(GPCommands.getRawCap("../SmartSafeServer/build/smartsafe/server/javacard/server.cap").toBytes());
			gp.installForInstallAndMakeSelectable(packAid, appAid, appAid, "", "");
		} catch (GPException e) {
			e.printStackTrace();
		}
	}
	public static void test() {
		SmartSafeAppli appli = new SmartSafeAppli(null);
		
		try {
			appli.coldReset();
			appli.select();
			//appli.authenticate("1233");
			//appli.authenticate("1234");
			appli.changePin("1234");
			appli.authenticate("1233");
			appli.authenticate("1234");
		} catch (GPException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			Scene scene = new Scene(GlobalView.createView(), 600, 400);
			primaryStage.setTitle("SmartSafe");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Throwable th) {
			th.printStackTrace();
			createExceptionDialog(th);
		}
	}
	protected Dialog<ButtonType> createExceptionDialog(Throwable th) {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("Program exception");
		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setContentText("Details of the problem:");
		dialogPane.getButtonTypes().addAll(ButtonType.OK);
		dialogPane.setContentText(th.getMessage());
		dialog.initModality(Modality.APPLICATION_MODAL);
		Label label = new Label("Exception stacktrace:");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		pw.close();
		TextArea textArea = new TextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		GridPane root = new GridPane();
		root.setVisible(false);
		root.setMaxWidth(Double.MAX_VALUE);
		root.add(label, 0, 0);
		root.add(textArea, 0, 1);
		dialogPane.setExpandableContent(root);
		dialog.showAndWait()
		.filter(response -> response == ButtonType.OK)
		.ifPresent(response -> System.exit(-1));
		return dialog;
	}
}
