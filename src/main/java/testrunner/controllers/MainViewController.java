package testrunner.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import testrunner.Configuration;
import testrunner.MainApp;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainViewController {
	public MainViewController() {
	}

	public static Logger log = LogManager.getLogger(MainViewController.class.getName());
	public TextField repoTxtFld;
	public Button workspaceBtn;
	public TextField workspaceTxtFld;
	public TextField branchTxtFld;
	public Button runBtn;
	public Button addParameterBtn;
	public VBox parametersVBox;
	public CheckBox localCkBox;
	public TextArea txtArea;
	private MainApp mainApp;
	private Configuration conf;

	public void addNewParameterControls() {
		addParameterControls("");
	}

	public void localRepoAction() {
		if (localCkBox.isSelected()) {
			repoTxtFld.setDisable(true);
			branchTxtFld.setDisable(true);
		} else {
			repoTxtFld.setDisable(false);
			branchTxtFld.setDisable(false);
		}
	}

	public void run() throws URISyntaxException {
		String repoUrl = repoTxtFld.getText().trim();
		File localDir = new File(workspaceTxtFld.getText().trim());

		if (!localDir.exists()) {
			localDir.mkdir();
		}

		List<String> commandsList = new ArrayList<>();

		if (!localCkBox.isSelected()) {
			URI url = new URI(repoUrl);
			String path = url.getPath();
			int i = path.lastIndexOf("/");
			String repoName = path.substring(i + 1, path.length() - 4);
			File projectDir = new File(localDir, repoName);
			System.out.println(projectDir.getAbsolutePath());
			if (projectDir.exists()) {
				System.out.println("Local repo seems to already exist. Will do a git pull");
				commandsList.add("cd " + projectDir);
				commandsList.add("git checkout " + branchTxtFld.getText().trim());
				commandsList.add("git pull");
			} else {
				System.out.println("Cannot find local repo. Will do a git clone");
				commandsList.add("cd " + localDir.getAbsolutePath());
				commandsList.add("git clone -b " + branchTxtFld.getText().trim() + " " + repoUrl);
			}
		} else {
			commandsList.add("cd " + localDir);
		}

		String mvnCommand = conf.getMvnCommand();

		List<String> parametersToPersist = new ArrayList<>();
		conf.setParameters(parametersToPersist);

		for (Node n : parametersVBox.getChildren()) {
			if (n instanceof GridPane) {
				TextField t1 = (TextField) ((GridPane) n).getChildren().get(0);
				TextField t2 = (TextField) ((GridPane) n).getChildren().get(2);

				mvnCommand = String.format("%s -D%s=%s", mvnCommand, t1.getText().trim(), t2.getText().trim());
				parametersToPersist.add(String.format("%s=%s", t1.getText().trim(), t2.getText().trim()));
			}
		}

		commandsList.add(mvnCommand.trim());

		String runnerBatFile = writeCommandsToFile(commandsList);

		List<String> argumentsList = new ArrayList<>();
		argumentsList.add("cmd.exe /k start");
		argumentsList.add(runnerBatFile);

		String args = argumentsList.stream().reduce((t, u) -> t + "," + u).get();
		argumentsList.forEach(this::printToTextArea);

		try {
			Runtime.getRuntime().exec(args);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

		conf.setParameters(parametersToPersist);
		saveConfiguration();
	}

	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
	}

	public void setRepoBranch() {
		conf.setRepoBranch(branchTxtFld.getText());
		saveConfiguration();
	}

	public void setRepoUrl() {
		conf.setRepoUrl(repoTxtFld.getText());
		saveConfiguration();
	}

	public void setWorkspacePath() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDir = directoryChooser.showDialog(mainApp.getPrimaryStage());
		String filePath = selectedDir.getAbsolutePath();
		workspaceTxtFld.setText(filePath);
		conf.setWorkspacePath(filePath);
		saveConfiguration();
	}

	private void addDefaultParameterControls() {
		conf.getParameters().forEach(this::addParameterControls);
	}

	private void addParameterControls(String parameterString) {
		String[] p = parameterString.split("=");
		String pName = "";
		String pValue = "";

		if(p.length > 0)
		{
			pName = p[0];
		}

		if(p.length > 1)
		{
			pValue = p[1];
		}

		GridPane parametersGrid = new GridPane();
		TextField pNameTxtFld = new TextField(pName);
		TextField pValueTxtFld = new TextField(pValue);
		Label label = new Label("=");
		Button removeParameterBtn = new Button("-");

		parametersGrid.setHgap(10);

		ColumnConstraints cc = new ColumnConstraints();
		cc.setPercentWidth(40);
		parametersGrid.getColumnConstraints().add(cc);

		cc = new ColumnConstraints();
		parametersGrid.getColumnConstraints().add(cc);

		cc = new ColumnConstraints();
		cc.setPercentWidth(40);
		parametersGrid.getColumnConstraints().add(cc);

		cc = new ColumnConstraints();
		parametersGrid.getColumnConstraints().add(cc);

		parametersGrid.add(pNameTxtFld, 0, 0);
		parametersGrid.add(label, 1, 0);
		parametersGrid.add(pValueTxtFld, 2, 0);
		parametersGrid.add(removeParameterBtn, 3, 0);

		parametersVBox.getChildren().add(1, parametersGrid);

		removeParameterBtn.setOnAction(event -> parametersVBox.getChildren().remove(parametersGrid));
	}

	private File getConfigurationFile() {
		File configurationFile = null;
		String appPath = null;
		try {
			appPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			File jarFile = new File(appPath);
			String runnerConfigPath = jarFile.getParent() + "/testrunner.json";
			configurationFile = new File(runnerConfigPath);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return configurationFile;
	}

	@FXML
	private void initialize() {
		loadConfiguration();
		repoTxtFld.setText(conf.getRepoUrl());
		branchTxtFld.setText(conf.getRepoBranch());
		workspaceTxtFld.setText(conf.getWorkspacePath());
		addDefaultParameterControls();
		printToTextArea(String.format("Hello %s!", System.getProperty("user.name")));
	}

	private void loadConfiguration() {
		ObjectMapper om = new ObjectMapper();
		try {
			conf = om.readValue(getConfigurationFile(), Configuration.class);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	private void printToTextArea(String text) {
		txtArea.appendText(System.getProperty("line.separator") + text);
	}

	private void saveConfiguration() {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(getConfigurationFile(), conf);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	private String writeCommandsToFile(java.util.List<String> commands) {
		File file = new File("runner.bat");
		PrintWriter writer;
		try {
			writer = new PrintWriter(file, "UTF-8");
			for (String s : commands) {
				writer.println(s);
				printToTextArea(s);
			}
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

		return file.getAbsolutePath();
	}
}
