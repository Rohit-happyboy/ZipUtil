package in.alqaholic.ZipUtil;

import com.jfoenix.controls.*;
import com.jfoenix.validation.RegexValidator;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class Main extends Application implements Initializable {

    ZipUtil zipper = new ZipUtil();

    private String file = "";


    private static void makeDraggableByNode(Stage target, Node draggable){
        Point p = new Point();

        draggable.setOnMousePressed(event -> p.setLocation(target.getX() - event.getScreenX(),target.getY() - event.getScreenY()));

        draggable.setOnMouseDragged(event -> {
            target.setX(p.getX()+event.getScreenX());
            target.setY(p.getY()+event.getScreenY());
        });
    }

    public static void main(String[] args) {
        if(args.length!=0)
            launch(Main.class,"--file="+args[0]+"");
        else
            launch(args);
    }

    private Stage stage;

    @Override
    public void init() throws Exception {
        super.init();

        Parameters parameters = getParameters();

        Map<String, String> params = parameters.getNamed();
        if(!params.isEmpty()){
            file = params.get("file");
        }

    }

    @Override
    public void start(Stage primaryStage) {

        this.stage = primaryStage;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("view.fxml"));
            JFXDecorator decorator = new JFXDecorator(primaryStage,root,false,false,true);
            makeDraggableByNode(primaryStage, decorator.getChildren().get(0));
            decorator.setTitle("Encryption Tool");

            Scene scene = new Scene(decorator);
            primaryStage.setScene(scene);
            scene.getStylesheets().add(Main.class.getResource("main.css").toExternalForm());
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("file = " + file);
        if(!file.isEmpty()){
            if(file.endsWith(".rx256")){
                file_input_decrypt.setText(file);
                tabs.getSelectionModel().select(decryptTab);
            }
            else {
                file_input_encrypt.setText(file);
                tabs.getSelectionModel().select(encryptTab);
            }
        }
    }



    @FXML
    void openFile(ActionEvent e){

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open a file to encrypt");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(stage);

        if(file == null) {
            file_input_encrypt.clear();
            return;
        }
        file_input_encrypt.setText(file.getAbsolutePath());
    }

    @FXML
    void openFolder(ActionEvent e){

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open a folder to encrypt");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = chooser.showDialog(stage);

        if(file == null) {
            file_input_encrypt.clear();
            return;
        }

        file_input_encrypt.setText(file.getAbsolutePath());
    }

    @FXML
    void openEncryptedFile(ActionEvent e){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open an encrypted file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("rx256","*.rx256"));
        File enFile = fileChooser.showOpenDialog(stage);

        if(enFile == null) return;


        if(enFile.exists()){
            file_input_decrypt.setText(enFile.getAbsolutePath());
        }
    }

    @FXML
    void encrypt(ActionEvent e){

        if(!new File(file_input_encrypt.getText()).exists()){
            showNotification("Invalid File...");
        }

        if(!secretKeyEncrypt.validate()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save encrypted file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("rx256","*.rx256"));
        File encryptedFile = fileChooser.showSaveDialog(stage);

        if(encryptedFile==null) return;

        try {
            Path temp_directory = Files.createTempDirectory("encrypted");
            ZipCipherUtil.encryptZip(file_input_encrypt.getText(),temp_directory.toString()+System.getProperty("file.separator")+"secret",secretKeyEncrypt.getText());
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(temp_directory.toString()+System.getProperty("file.separator")+"properties"));
            String[] data = {securityQuestion.getText(),answer.getText(),secretKeyEncrypt.getText()};
            oos.writeObject(data);
            oos.close();
            zipper.zip(temp_directory.toString(),encryptedFile.toString());
            showNotification("File Encrypted!");
            Files.delete(new File(temp_directory.toString()+System.getProperty("file.separator")+"secret").toPath());
            Files.delete(new File(temp_directory.toString()+System.getProperty("file.separator")+"properties").toPath());
            Files.delete(temp_directory);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @FXML
    void decrypt(ActionEvent e){

        if(!new File(file_input_decrypt.getText()).exists()){
            showNotification("Invalid File...");
        }

        if(!secretKeyDecrypt.validate()) return;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Save decrypted files to...");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File decryptedFile = directoryChooser.showDialog(stage);

        if(decryptedFile==null) return;



        try {
            Path temp_directory = Files.createTempDirectory("encrypted");

            zipper.unZip(file_input_decrypt.getText(),temp_directory.toString());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(temp_directory.toString()+System.getProperty("file.separator")+"properties"));
            String[] data = (String[]) ois.readObject();
            try {
                ZipCipherUtil.decryptUnzip(temp_directory.toString()+System.getProperty("file.separator")+"secret",decryptedFile.toString(),secretKeyDecrypt.getText());
                showNotification("File Decrypted!");
            } catch (Exception e1) {
                showForgotPasswordDialog(data);
            }
            Files.delete(new File(temp_directory.toString()+System.getProperty("file.separator")+"secret").toPath());
            Files.delete(new File(temp_directory.toString()+System.getProperty("file.separator")+"properties").toPath());
            Files.delete(temp_directory);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void showNotification(String msg){
        JFXDialogLayout dialogLayout = new JFXDialogLayout();

        Text heading = new Text(msg);
        heading.setFont(Font.font(16));
        JFXButton ok = new JFXButton("OK");
        ok.setTextFill(Color.GREEN);

        dialogLayout.setActions(ok);
        dialogLayout.setHeading(heading);

        JFXDialog dialog = new JFXDialog();
        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        dialog.setContent(dialogLayout);
        dialog.show(root);

        ok.setOnAction(event -> {
            dialog.close();
        });

    }

    private void showForgotPasswordDialog(String[] data){
        JFXDialogLayout dialogLayout = new JFXDialogLayout();

        Text heading = new Text("Invalid Password");
        heading.setFill(Color.RED);

        JFXButton cancel = new JFXButton("Cancel");
        cancel.setTextFill(Color.RED);
        JFXButton recover = new JFXButton("Recover");
        recover.setTextFill(Color.GREEN);
        JFXTextField answer = new JFXTextField();
        answer.setPromptText(data[0]);
        answer.setLabelFloat(true);

        dialogLayout.setActions(cancel,recover);
        dialogLayout.setHeading(heading);
        dialogLayout.setBody(answer);

        JFXDialog dialog = new JFXDialog();
        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        dialog.setContent(dialogLayout);
        dialog.show(root);

        cancel.setOnAction(event -> {
            secretKeyDecrypt.clear();
            dialog.close();
        });

        recover.setOnAction(event -> {
            if(answer.getText().equalsIgnoreCase(data[1])){
                secretKeyDecrypt.setText(data[2]);
                dialog.close();
                showNotification("Password Recovered!");
            }
            else {
                showNotification("Invalid Answer!");
                answer.clear();
            }
        });
    }

    @FXML
    public StackPane root;
    @FXML
    public JFXTabPane tabs;
    @FXML
    public JFXTextField securityQuestion;
    @FXML
    public JFXTextField answer;
    @FXML
    public JFXTextField file_input_encrypt;
    @FXML
    public JFXTextField file_input_decrypt;
    @FXML
    public JFXTextField secretKeyEncrypt;
    @FXML
    public JFXTextField secretKeyDecrypt;
    @FXML
    public Tab encryptTab;
    @FXML
    public Tab decryptTab;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        RegexValidator regexValidator = new RegexValidator();
        regexValidator.setRegexPattern("\\w{6,16}");
        regexValidator.setMessage("Invalid length (6-16)");
        secretKeyEncrypt.getValidators().add(regexValidator);
        secretKeyDecrypt.getValidators().add(regexValidator);



    }
}
