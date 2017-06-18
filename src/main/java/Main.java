import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.event.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.*;

public class Main extends Application {
	/** Application name. */
	private static final String APPLICATION_NAME =
			"Texture Mapper";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY =
			JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials
	 * at ~/.credentials/sheets.googleapis.com-java-quickstart
	 */
	private static final List<String> SCOPES =
			Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);
	
	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in =
				Main.class.getResourceAsStream("client_secret.json");
		GoogleClientSecrets clientSecrets =
				GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow =
				new GoogleAuthorizationCodeFlow.Builder(
						HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(DATA_STORE_FACTORY)
				.setAccessType("offline")
				.build();
		Credential credential = new AuthorizationCodeInstalledApp(
				flow, new LocalServerReceiver()).authorize("user");
		System.out.println(
				"Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Build and return an authorized Sheets API client service.
	 * @return an authorized Sheets API client service
	 * @throws IOException
	 */
	public static Sheets getSheetsService() throws IOException {
		Credential credential = authorize();
		return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}

	public List<String> parseString(String input){
		String[] subresult = input.split("\\n|\\\\|_|\\.");
		List<String> result = new ArrayList<>();
		String currentCrc = "";
		for(String i : subresult) {
			if(!currentCrc.equals(i) && i.matches("^(0x)[a-fA-F0-9]{8}$")) { // remove duplicates
				currentCrc = i;
				result.add(i);
			}
		}
		return result;
	}

	private CheckBox renameDupes;
	private CheckBox matchAuthor;
	private CheckBox portSoloTextures;
	
	public boolean getRenameDupes() {
		return renameDupes.isSelected();
	}
	
	public boolean getMatchAuthor() {
		return matchAuthor.isSelected();
	}
	
	public boolean getPortSoloTextures() {
		return portSoloTextures.isSelected();
	}
	
	public static void main(String[] args){
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		/*
		 * SETUP
		 */

		// Build a new authorized API client service.
		Sheets service = getSheetsService();

		// Fetch the spreadsheet
		String[] spreadsheetIds = {"1Gvnz_trNOUgW6CSI3eeEYk7SvqsSklIgVuoHDlmw8e8", "1VZKgxDAfASaZVcNHbeIFEClxRxn3v2-sNwSOEvgYHrs"}; // texture map, authors
		String textureMapRange = "Texture_Map_Full!A2:K";
		String me2AuthorsRange = "ALOT_ME2!A4:B";
		String me3AuthorsRange = "ALOT_ME3!A4:B";
		
		ValueRange textureMap = service.spreadsheets().values()
				.get(spreadsheetIds[0], textureMapRange)
				.execute();
		// Build database of textures
		List<List<Object>> textureMapValues = textureMap.getValues();
		
		ValueRange authorsME2 = service.spreadsheets().values()
				.get(spreadsheetIds[1], me2AuthorsRange)
				.execute();
		List<List<Object>> textureAuthorsME2 = authorsME2.getValues();
		
		ValueRange authorsME3 = service.spreadsheets().values()
				.get(spreadsheetIds[1], me3AuthorsRange)
				.execute();
		List<List<Object>> textureAuthorsME3 = authorsME3.getValues();
		
		Database database = new Database(textureMapValues, textureAuthorsME2, textureAuthorsME3);

		/*
		 * START
		 */

		primaryStage.setTitle("Texture Mapper");

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER); // position from top left of the scene
		grid.setHgap(10); // spacing between rows
		grid.setVgap(10); // btw columns
		grid.setPadding(new Insets(10, 10, 10, 10)); // padding per side

		/*
		 * LEFT
		 */

		GridPane leftGrid = new GridPane();

		Label original = new Label(" Original Textures");

		TextArea originalText = new TextArea();
		originalText.setPromptText("Paste your texture's hashes here, one per line.");

		Button btnDetectDupes = new Button("Go!");
		btnDetectDupes.setMinSize(60, Button.USE_PREF_SIZE);

		ToolBar toolbarLeft = new ToolBar(new Spacer(), btnDetectDupes);
		toolbarLeft.setPadding(new Insets(0,0,0,0));
		toolbarLeft.setBackground(null);

		leftGrid.add(original, 0, 1);
		leftGrid.add(originalText, 0, 2, 2, 1);
		leftGrid.add(toolbarLeft, 1, 3);

		leftGrid.setAlignment(Pos.CENTER);
		leftGrid.setHgap(10);
		leftGrid.setVgap(10);

		/*
		 * RIGHT
		 */

		GridPane rightGrid = new GridPane();

		ComboBox<String> gameList = new ComboBox<String>();
		gameList.getItems().setAll("ME1", "ME2", "ME3");
		gameList.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		GridPane.setFillWidth(gameList, true);
		gameList.setPromptText("Game");

		Label converted = new Label(" Mapped Textures");

		TextArea duplicateText = new TextArea();
		duplicateText.setEditable(false);

		Label error = new Label();
		
		matchAuthor = new CheckBox();
		matchAuthor.setText("Match");
		matchAuthor.setTooltip(new Tooltip("Match texture's author when applicable\n"
				+ "Don't use this when the rename option is disabled (inaccurate results)"));
		
		portSoloTextures = new CheckBox();
		portSoloTextures.setText("Solo");
		portSoloTextures.setTooltip(new Tooltip("Port solo textures\n"
				+ "Disable when you only want the duplicates from a set of textures\n"
				+ "Enable to get e.g. a full self-port of your mod from MEx to MEx"));

		renameDupes = new CheckBox();
		renameDupes.setText("Rename");
		renameDupes.setTooltip(new Tooltip("Rename (don't overwrite) multiple duplicates"));


		ToolBar toolbarRight = new ToolBar(new Spacer(), error, renameDupes, portSoloTextures, matchAuthor, gameList);
		toolbarRight.setPadding(new Insets(0,0,0,0));
		toolbarRight.setBackground(null);

		rightGrid.add(converted, 0, 1);
		rightGrid.add(duplicateText, 0, 2, 2, 1);
		rightGrid.add(toolbarRight, 1, 3);

		rightGrid.setAlignment(Pos.CENTER);
		rightGrid.setHgap(10);
		rightGrid.setVgap(10);

		/*
		 * LISTENERS
		 */
		
		// Don't use match and rename at the same time, results would be inaccurate
		renameDupes.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {
				matchAuthor.setDisable(renameDupes.isSelected() ? false : true);
				matchAuthor.setSelected(false);
			}
		});

		btnDetectDupes.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {
				error.setText(""); // reset error message
				String crcs = originalText.getText();

				if(crcs.length() != 0) {
					String selectedGame = gameList.getValue();
					if(selectedGame != null) {
						List<String> crcList = parseString(crcs);

						FileWriter write = null;
						try {
							write = new FileWriter(selectedGame + ".bat", false);
							write.write("");
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						PrintWriter print_line = new PrintWriter(write);

						duplicateText.setText("");
						print_line.println("mkdir " + selectedGame);

						Map<String, Integer> processedHashes = new HashMap<>();
						for(String sourceHash : crcList) {
							List<List<String>> crcDuplicates = database.search(sourceHash, selectedGame, getPortSoloTextures(), getMatchAuthor());
							if(crcDuplicates != null) { // if texture can't be found in the texture map, skip it
								for(List<String> crc : crcDuplicates){
									String destinationHash = crc.get(0);
									String textureClass = crc.get(1);
									String author = getMatchAuthor() ? crc.get(2) : "";
									
									/*
									 * Special modes
									 * - Copy textures in subfolders, following author
									 * - Don't port solo (not portable to another game) textures
									 * - Rename duplicates (no overwriting)
									 */
									
									if(getRenameDupes()) { 
										Integer duplicateCount = processedHashes.get(destinationHash);
										if(duplicateCount != null){ // if hash has already been processed
											duplicateCount++;
											processedHashes.put(destinationHash, duplicateCount);
											destinationHash += "_" + duplicateCount; // add a number to the file
										} else {
											processedHashes.put(destinationHash, 1);
										}
									}
									duplicateText.appendText(sourceHash + " -> " + destinationHash + " (" + textureClass + ") " + author); // crc and class
									duplicateText.appendText("\n");
									
									if(getMatchAuthor())
										destinationHash += "\\" + author;
									
									print_line.println("copy " + sourceHash + ".dds " + selectedGame + "\\" + destinationHash + ".dds");    								
								}
								duplicateText.appendText("\n");
							}
						}
						print_line.close();
					} else {
						error.setText("No game selected.");
					}
				} else {
					error.setText("No textures to process.");
				}
			}
		});

		/*
		 * END
		 */

		grid.add(leftGrid, 0, 0);
		grid.add(rightGrid, 1, 0);

		RowConstraints emptyRow = new RowConstraints();
		RowConstraints rowConstraints = new RowConstraints();
		rowConstraints.setVgrow(Priority.ALWAYS);
		leftGrid.getRowConstraints().add(emptyRow); // aucune propriété sur la première ligne
		leftGrid.getRowConstraints().add(emptyRow); // ni sur la 2ème
		leftGrid.getRowConstraints().add(rowConstraints); // scale pour la 3ème
		rightGrid.getRowConstraints().add(emptyRow);
		rightGrid.getRowConstraints().add(emptyRow);
		rightGrid.getRowConstraints().add(rowConstraints);
		grid.getRowConstraints().add(rowConstraints);

		ColumnConstraints emptyCol = new ColumnConstraints();
		ColumnConstraints columnConstraints = new ColumnConstraints();
		columnConstraints.setHgrow(Priority.ALWAYS);
		leftGrid.getColumnConstraints().add(emptyCol);
		leftGrid.getColumnConstraints().add(columnConstraints);
		rightGrid.getColumnConstraints().add(emptyCol);
		rightGrid.getColumnConstraints().add(columnConstraints);
		grid.getColumnConstraints().add(columnConstraints);
		grid.getColumnConstraints().add(columnConstraints); // appliquer sur les 2 colonnes -> 50% chacune

		Scene scene = new Scene(grid);
		primaryStage.setScene(scene);

		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		primaryStage.setMinHeight(screenBounds.getHeight()/3);
		primaryStage.setMinWidth(screenBounds.getWidth()/3);

		primaryStage.show();
	}
}