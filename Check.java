
/*
Name: Seismic Phase Inspection Visualization Platform
Function: Check the results of automatic pickup
Time: 2022.12.1
Author: Jh.Xaing

Logs:2022.12.21  --  Set the discard and retain functions to automatically generate the imaging format
     2023.1.7    --  Increase the revocation, adjustment function, improve the discard, retain function.
     2023.1.9    --  Increase the roller adjustment function
     2023.1.11   --  Increase memory function
     2023.2.1    --  Increase the epicentral distance mode
     2023.2.2    --  Increase the preview function
     2023.2.11   --  Increase the check history display function
     2023.2.12   --  Interface beautification 
     2023.3.27   --  Increase the detection-free function
     2023.3.27   --  Improve the epicentral distance function
     2023.4.28   --  Add 'Sn' seismic phase and add subsequent seismic phase expansion tools.
     2023.6.21   --  Add the home page, expand the function of configuring parameters in advance
*/


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Check extends Application {
    // 'Application' is the entry point of 'JavaFX' program. Any 'JavaFX' program must inherit this class and rewrite methods such as 'start ( )'.
    
    // Type of seismic phase
    static Integer phaseType ;
    static int phaseTypeInt;
    static String phaseTypeString;

    // Screening level
    static String level ;
    // Minimum epicentral distance
    static float minGcarc ;
    // Maximum epicentral distance
    static float maxGcarc ;

    // Picture file path
	static String imagedir0;
    // Text file path
    static String inFile0;
    // Export path
    static String outFile0 ;

    // Open the epicentral distance mode, the default time mode
    static Boolean epiModel ;
    // Whether to turn on the order recovery function, close by default (self-start after completing all filters)
    static Boolean recoverSort;

    // Load check history
    static Boolean historyLoad ;
    // Pick-up check file
    static String historyFile ;

    // Load check-free function
    static Boolean exemptionLoad;
    // Check-free file
    static String exemptionFile;
    
    // Detail file
    static String rawFile ;
    static String flowtempFile ;
    static String firmtempFile ;
    static String rawsorttempFile ;
    static String imagedir;
    static String outFile ;

    private ImageView imageview = new ImageView();  
	private String[] allLinesList;
    private float[] peaksTimeAndAnchorList;
    private float[] peaksIdxList;
    private List<Integer> peaksIdxLogList = new ArrayList<>();
    private static List<Integer> peaksIdxHistoryList = new ArrayList<>();
	private Integer index=0;
    private Integer pick_index = 0;
    private float  pick_time = 0;
    private final Integer CanvasHeight= 815;
    private static Integer CanvasWidth;
    private static Double CanvasLeft;
    private static String nowProgress;
    static Boolean DeveloperMode = false;
    static String configsdir;

    public static void ConfigsRead() throws IOException{
        try{
            // Read the configuration file
            Properties prop = new Properties();
            // Get the input stream object that reads the 'properties' file
            InputStream is =Check.class.getClassLoader().getResourceAsStream(configsdir);
            // The 'properties' file is read and parsed by a given input stream object
            prop.load(is);
            // Get the contents of the 'properties' file
    
            // Type of seismic phase
            phaseType = Integer.valueOf(prop.getProperty("phaseType"));
            // Screening level
            level = prop.getProperty("level");
            // Minimum epicentral distance
            minGcarc = Float.parseFloat(prop.getProperty("minGcarc"));
            // Maximum epicentral distance
            maxGcarc = Float.parseFloat(prop.getProperty("maxGcarc"));
    
            // Picture file path
            imagedir0 = prop.getProperty("imagedir");
            // Text file path
            inFile0 = prop.getProperty("inFile");
            // Export path
            outFile0 = prop.getProperty("outFile");
    
            // Whether to open the epicentral distance mode
            epiModel = Boolean.valueOf(prop.getProperty("epiModel"));
            // Whether to turn on the order recovery function
            recoverSort = Boolean.valueOf(prop.getProperty("recoverSort"));
    
            // Does load check history
            historyLoad=Boolean.valueOf(prop.getProperty("historyLoad"));
            // Check history file path
            historyFile=prop.getProperty("historyFile");
    
            // Whether to turn on the Check-free function
            exemptionLoad=Boolean.valueOf(prop.getProperty("exemptionLoad"));
            // Check-free file path
            exemptionFile=prop.getProperty("exemptionFile");
    
            // // Input and output file path
            // rawFile = prop.getProperty("rawFile");
            // outFile = prop.getProperty("outFile");
            // // Temporary file path
            // flowtempFile = prop.getProperty("flowtempFile");
            // firmtempFile = prop.getProperty("firmtempFile");
            // rawsorttempFile = prop.getProperty("rawsorttempFile");
    
        } catch (Exception e) {
            System.out.println("Failed to read configuration file !");
        }
    }

    public static void Pretreat() throws IOException{
        // Seismic phase judgment
        try {
            PhaseJudge();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Display the current time
        try {
            PrintTime("Start");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Pretreatment...");
        
        // Identify progress
        try {
            FindProgress();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Primary screening
        if (epiModel == true){
            try {
                FirstScreen1();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                FirstScreen2();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Read the check history
        if (historyLoad){
            try {
                HistoryRead();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    
    public static void PhaseJudge() throws IOException
    {
        phaseTypeInt = phaseType;
        switch ( phaseTypeInt ){
            case 1 :
            phaseTypeString = "Pn";
            CanvasWidth = 1298;CanvasLeft= 124.0;
            System.out.println(phaseTypeString+"Seismic phase check");
            break;
            case 2 :
            phaseTypeString = "Sn";
            CanvasWidth = 1309;CanvasLeft= 112.0;
            System.out.println(phaseTypeString+"Seismic phase check");
            break;
            default :
            phaseTypeString = "";
            System.out.println("Phase input error, please inspect the configuration file !");
        }
        rawFile = inFile0 + '/' + phaseTypeString + '/' + phaseTypeString + ".txt";
        flowtempFile = outFile0 + '/' + phaseTypeString + '/' + "flowtemp" + ".txt";
        firmtempFile = outFile0 + '/' + phaseTypeString + '/' + "firmtemp" + ".txt";
        rawsorttempFile = outFile0 + '/' + phaseTypeString + '/' + "rawsorttemp" + ".txt";
        imagedir = imagedir0 + '/' + phaseTypeString + '/' ;
        outFile = outFile0 + '/' + phaseTypeString + '/' + phaseTypeString + "_picknet_manu" + ".txt";

        File file1 =new File(outFile0);
        if (!file1 .exists() && !file1 .isDirectory()){
            file1.mkdir();
        } 
        File file2 =new File(outFile0 + '/' + phaseTypeString);
        if (!file2 .exists() && !file2 .isDirectory()){
            file2.mkdir();
        } 
    }

    public static void PrintTime(String tip) throws IOException
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        System.out.println(tip+":  "+dateFormat.format(date));  
    }

    public static void Exemption()throws Exception{  
        System.out.println("Read check-free files...");
        File file=new File(exemptionFile);  
        if(!file.exists()){
            exemptionLoad = false;
            System.out.println("Check-free file does not exist !");
        } else {
            long begin=System.currentTimeMillis();
            String inputFile = rawFile;
            String outputFile = flowtempFile;
            List<String> allLines = Files.readAllLines(Paths.get(inputFile));
            List<String> allLinesExemption = Files.readAllLines(Paths.get(exemptionFile));
            // Define the output file path
            FileWriter fruitf = new FileWriter(outputFile);
            String line = System.getProperty("line.separator");
            StringBuffer fruit = new StringBuffer();
            for (String lines : allLines){
                Boolean remain = true;
                String[] linelist =  lines.trim().split("\\s+");
                String ray = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
                for (String lineexemption : allLinesExemption){
                    String[] linelist1 =  lineexemption.trim().split("\\s+");
                    String rayexempt = String.join("  ", linelist1[0], linelist1[1], linelist1[2], linelist1[3]);
                    if (ray.equals(rayexempt)){
                        remain = false;
                        break;
                    }
                }
                if (remain){
                    fruit.append(lines).append(line);
                }
            }
            fruitf.write(fruit.toString());
            fruitf.close();
            long end=System.currentTimeMillis();
            System.out.println("The end of the check-free file reading, time (s) :"+ (end - begin)/1000);
        }
    }

    public static void FindProgress() throws Exception{
        String formerFile0 = firmtempFile;
        File formerFile = new File(formerFile0);
        if (formerFile.exists()){
            List<String> allLines = Files.readAllLines(Paths.get(formerFile0));
            if (allLines.size()!=0){
                String[] linelist =  allLines.get(allLines.size()-1).trim().split("\\s+");
                nowProgress = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
                System.out.println("Continue to check from here :" + nowProgress);
            } else {
                nowProgress = "";
                System.out.println("Restart check");
            }
        } else {
            nowProgress = "";
            System.out.println("Restart check");
        }
    }

    public static void FirstScreen1() throws Exception{
        File rawsortFile0 = new File(rawsorttempFile);
        if (! rawsortFile0.exists()){
            String inputFile;
            if (exemptionLoad){
                Exemption();
                inputFile = flowtempFile;
                if (!exemptionLoad){
                    inputFile = rawFile;
                }
            } else {
                inputFile = rawFile;
            }
            
            String outputFile = rawsorttempFile;

            //// Screening and statistics
            // Convert string to list
            String[] levellist = level.split("");

            List<String> allLines = Files.readAllLines(Paths.get(inputFile));
            List<String> allLinesscreen = new ArrayList<>();

            Integer a = 0;
            Integer b = 0;
            Integer c = 0;
            Integer d = 0;
            Integer outsum = 0;
            Integer sum = 0;
            Integer aarc = 0;
            Integer barc = 0;
            Integer carc = 0;
            Integer darc = 0;
            Integer sumarc = 0;

            for (String lines : allLines)
            {
                sum ++;
                String[] linelist =  lines.trim().split("\\s+");
                String linelevel = linelist[20];
                float linegcarc = Float.valueOf(linelist[5]);

                for (String lev : levellist)
                {   
                    if (lev.equals(linelevel) && (linegcarc >= minGcarc) && (linegcarc < maxGcarc))
                    {
                        // System.out.println(linelevel);
                        String output = lines;
                        allLinesscreen.add(output);
                        outsum ++; 
                    }
                }
                if (linelevel.equals(String.valueOf('A')))
                {
                    a ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        aarc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('B')))
                {
                    b ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        barc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('C')))
                {
                    c ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        carc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('D')))
                {
                    d ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        darc ++;   
                    }
                }
            }
            sumarc = aarc + barc + carc + darc ;

            System.out.println("Statistics : A/B/C/D   Counted as ([" + minGcarc + ","+maxGcarc+")): "+ a +"("+ aarc +") / " + b +"("+ barc +") / "+ c +"("+ carc +") / "+ d+"("+ darc +")");
            System.out.println("Original rays Number ([" + minGcarc + ","+maxGcarc+")): "+ sum+"("+ sumarc +")");
            System.out.println("Screening out : "+ level+" and " + "[" + minGcarc + ","+maxGcarc+")" + "   Rays Number: "+ outsum);
            //// Sort
            String[] allLineslist = allLinesscreen.toArray(new String[allLinesscreen.size()]);

            // Select sort algorithm
            System.out.println("New sort...");
            long begin=System.currentTimeMillis();
            for (int i = 0; i < allLineslist.length; i++) {
                int minIndex = i;
                // Compares the number of the current traversal with all the subsequent numbers, and records the subscript of the smallest number.
                for (int j = i + 1; j < allLineslist.length; j++) {
                    if (Float.valueOf(allLineslist[j].trim().split("\\s+")[5]) < Float.valueOf(allLineslist[minIndex].trim().split("\\s+")[5])) 
                    {
                        // Record the subscript of the smallest number
                        minIndex = j;
                    }
                }
                // If the smallest number is inconsistent with the current traversal's subscript, the exchange
                if (i != minIndex) {
                    String temp = allLineslist[i];
                    allLineslist[i] = allLineslist[minIndex];
                    allLineslist[minIndex] = temp;
                }
            }
            long end=System.currentTimeMillis();

            List<String> allLinesArrays = Arrays.asList(allLineslist);

            // Define the output file path
            FileWriter fruitf = new FileWriter(outputFile);
            String line = System.getProperty("line.separator");
            StringBuffer fruit = new StringBuffer();

            for (String lines : allLinesArrays)
            {
                String output = lines;
                fruit.append(output).append(line);
            }

            fruitf.write(fruit.toString());
            fruitf.close();

            System.out.println("Sorting by epicentral distance has been completed, time(s): " + (end - begin)/1000);

        } else if (rawsortFile0.exists()) {
            System.out.println("According to the existing sort");
        }

        //// Restore memory and statistics
        String inputFile = rawsorttempFile;
        String outputFile = flowtempFile;
        List<String> allLines = Files.readAllLines(Paths.get(inputFile));
        // Define the output file path
        FileWriter fruitf = new FileWriter(outputFile);
        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();

        Boolean location = false;
        Integer a = 0;
        Integer b = 0;
        Integer c = 0;
        Integer d = 0;
        double nowGcarc = (float) 0;
        double[] region = {(float)0,(float)0.1,(float)0.2,(float)0.3};
        double[] region1 = new double[4];
        Integer outsum = 0;

        for (String lines : allLines){
            String[] linelist =  lines.trim().split("\\s+");
            String nowProgress1 = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
            if ( nowProgress.length()==0 ||  nowProgress.equals(nowProgress1) ){
                nowProgress = "used";
                location = true;
                nowGcarc = Float.valueOf(linelist[5]);
                nowGcarc = Math.floor((nowGcarc*10+0.5))/10;
                for (int i = 0; i < region.length; i++){
                    double nowGcarc1 ;
                    nowGcarc1 = Math.floor(((nowGcarc + region[i])*10+0.5))/10;
                    region1[i] = nowGcarc1;
                }
            }

            if (location){
                String output = lines;
                fruit.append(output).append(line);
                outsum ++; 
                double linesGcarc = Float.valueOf(linelist[5]);
                linesGcarc = Math.floor(linesGcarc*10+0.5)/10;
                if (linesGcarc == region1[0]){
                    a++;
                } else if (linesGcarc == region1[1]){
                    b++;
                } else if (linesGcarc == region1[2]){
                    c++;
                } else if (linesGcarc == region1[3]){
                    d++;
                }
            }
        }

        fruitf.write(fruit.toString());
        fruitf.close();
        System.out.println("Remaining rays number: "+ outsum);
        System.out.println("Preview:  "+ region1[0]+":"+a + "   " + region1[1] +":"+b + "   " + region1[2] +":"+c + "   " + region1[3] +":"+d );
    }

    public static void FirstScreen2() throws Exception
    {
        String inputFile;
        if (exemptionLoad){
            Exemption();
            inputFile = flowtempFile;
            if (!exemptionLoad){
                inputFile = rawFile;
            }
        } else {
            inputFile = rawFile;
        }
        String outputFile = flowtempFile;

        // Convert string to list
        String[] levellist = level.split("");
        //System.out.println(levellist[1]);
        List<String> allLines = Files.readAllLines(Paths.get(inputFile));
        // Define the output file path
        FileWriter fruitf = new FileWriter(outputFile);

        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();
        Integer a = 0;
        Integer b = 0;
        Integer c = 0;
        Integer d = 0;
        Integer outsum = 0;
        Integer sum = 0;
        Integer aarc = 0;
        Integer barc = 0;
        Integer carc = 0;
        Integer darc = 0;
        Integer sumarc = 0;
        Boolean location = false;

        for (String lines : allLines)
        {   
            String[] linelist =  lines.trim().split("\\s+");
            String nowProgress1 = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
            if ( nowProgress.length()==0 ||  nowProgress.equals(nowProgress1) ){
                location = true;
            }

            if (location){
                sum ++;
                String linelevel = linelist[20];
                float linegcarc = Float.valueOf(linelist[5]);
                for (String lev : levellist)
                {   
                    if (lev.equals(linelevel) && (linegcarc >= minGcarc) && (linegcarc < maxGcarc))
                    {
                        // System.out.println(linelevel);
                        String output = lines;
                        fruit.append(output).append(line);
                        outsum ++; 
                    }
                }
                if (linelevel.equals(String.valueOf('A')))
                {
                    a ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        aarc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('B')))
                {
                    b ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        barc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('C')))
                {
                    c ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        carc ++;   
                    }
                }
                if (linelevel.equals(String.valueOf('D')))
                {
                    d ++;
                    if ((linegcarc >= minGcarc) && (linegcarc < maxGcarc)){
                        darc ++;   
                    }
                }
            }
           
        }
        sumarc = aarc + barc + carc + darc ;


        fruitf.write(fruit.toString());
        fruitf.close();
        System.out.println("Statistics : A/B/C/D   Counted as ([" + minGcarc + ","+maxGcarc+")): "+ a +"("+ aarc +") / " + b +"("+ barc +") / "+ c +"("+ carc +") / "+ d+"("+ darc +")");
        System.out.println("Original rays Number ([" + minGcarc + ","+maxGcarc+")): "+ sum+"("+ sumarc +")");
        System.out.println("Screening out: "+ level+" and " + "[" + minGcarc + ","+maxGcarc+")" + "   Rays number: "+ outsum);
    }

    public static void HistoryRead() throws IOException
    {
        System.out.println("Read check history file...");
        File file=new File(historyFile);  
        if(!file.exists()){
            historyLoad = false;
            System.out.println("Check history file does not exist !");
        } else {
            long begin=System.currentTimeMillis();
            String inputFile = flowtempFile;
            List<String> allLines = Files.readAllLines(Paths.get(inputFile));
            List<String> allLinesHistory = Files.readAllLines(Paths.get(historyFile));
            // Traverse all flowing temporary files
            for (int i=0; i<allLines.size(); i++){
                // Split into lists
                String[] linelist =  allLines.get(i).trim().split("\\s+");
                // Intercept arrival times List
                List<String> peak1 = new ArrayList<>();
                for (int j=25; j<linelist.length; j++){
                    peak1.add(linelist[j]);
                }
                String[] peak2 = peak1.toArray(new String[peak1.size()]);
                
                peaksIdxHistoryList.add(peak2.length);
                // Traverse all pickup history
                for (String linesHistory : allLinesHistory){
                    String[] lineHistorylist =  linesHistory.trim().split("\\s+");
                    String rayHistory = String.join("  ", lineHistorylist[0], lineHistorylist[1], lineHistorylist[2], lineHistorylist[3]);
                    String ray = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
                    // Find the records that exist
                    if (ray.equals(rayHistory)){
                        String arrivalHistory = lineHistorylist[8];
                        // System.out.println(arrivalHistory);
                        // Traversing all possible arrival times
                        for (int k=0; k<peak2.length; k++){
                            String arrival = peak2[k];
                            // Find the same arrival time
                            if (arrival.equals(arrivalHistory)){
                                // Write to time index
                                peaksIdxHistoryList.set(i,k);
                            }
                        }
                    }
                }
                //System.out.println(peaksIdxHistoryList.get(i));
            }
            long end=System.currentTimeMillis();
            System.out.println("End of reading check history file, time (s): "+ (end - begin)/1000);
            //System.out.println(peaksIdxHistoryList.size());
        }
    }

    // Rewrite class  Generally used for initialization work
    @Override
    public void init() throws Exception {
        System.out.println("After confirming the configuration, click the 'Start' button to continue...");
    }

    //Rewrite class
    @Override
    public void start(Stage stage) throws Exception {            // The parameter is a stage instance
        Locale.setDefault(Locale.ENGLISH);
        // The stage size is variable
        stage.setResizable(false);
        // Stage style
        stage.initStyle(StageStyle.DECORATED);
        
        // Define button
        Button button_begin1 = new Button("Start");
        // Position
        //button1.setLayoutY(500);button1.setLayoutX(250);
        // Style
        button_begin1.setStyle("-fx-background-color: white; -fx-background-radius:10; -fx-border-color: black; -fx-border-width: 1px ; -fx-border-radius:10");
        // Dimension
        button_begin1.setPrefWidth(150);button_begin1.setPrefHeight(70);
        // Font
        button_begin1.setFont(Font.font("Times New Roman",FontWeight.BOLD,25));// Application system font, bold, size 30, as button font
        // Cursor
        button_begin1.setCursor(Cursor.CLOSED_HAND);

        // Define button
        Button button_begin2 = new Button("Configuration");
        // Position
        //button1.setLayoutY(500);button1.setLayoutX(250);
        // Style
        button_begin2.setStyle("-fx-background-color: white; -fx-background-radius:1; -fx-border-color: black; -fx-border-width: 1px ; -fx-border-radius:10");
        // Dimension
        button_begin2.setPrefWidth(150);button_begin2.setPrefHeight(70);
        // Font
        button_begin2.setFont(Font.font("Times New Roman",FontWeight.BOLD,18));// Application system font, bold, size 30, as button font
        // Cursor
        button_begin2.setCursor(Cursor.CLOSED_HAND);

        // Define labels
        Label label = new Label("Seismic Phase Inspection\n Visualization Platform\n      welcome you");
        label.setAlignment(javafx.geometry.Pos.CENTER);
        label.setFont(Font.font("Times New Roman",FontWeight.BOLD,55));
        label.setMaxWidth(870);
        label.setWrapText(true);


        
        // Define Start Layout
        HBox hbox0 = new HBox();              // Horizontal box layout
        hbox0.setPadding(new Insets(10, 270, 65, 270)); // Distance from node to edge
        hbox0.setSpacing(40); // Spacing between nodes
        BorderPane borderpane0 = new BorderPane();  // Upper and lower left and right middle layout
        borderpane0.setBottom(hbox0);
        hbox0.getChildren().addAll(button_begin1, button_begin2);
        borderpane0.setBottom(hbox0);
        borderpane0.setCenter(label);

        //Create scene
        Scene beginscene =  new Scene(borderpane0,900,600);// Put the layout into the scene and set the width and height

        // Put scene into stage
        stage.setScene(beginscene);

        // Define start event       
        button_begin1.setOnAction(event ->{

            // Read configuration file
            try {
                ConfigsRead();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // Pretreatment
            try {
                Pretreat();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            stage.hide();

            // Child window
            try {
                Subwindow();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }); 

        // Define configuration event      
        button_begin2.setOnAction(event ->{
            try {
                String file = configsdir; // file path
                Process p = Runtime.getRuntime().exec("gedit " + file); // Execute the command line to open the file
                p.waitFor(); // Wait for the completion of the program execution
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }); 

        // Exit protection reminder
        Platform.setImplicitExit(false);
        stage.setOnCloseRequest(event -> {
            event.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit");
            alert.setHeaderText(null);
            alert.setContentText("Do you want to end the check ?");
            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == ButtonType.OK){
                Platform.exit(); 
            }
        });
        // Show stage
        stage.show();
    }

        private final Stage stage = new Stage();
        public void Subwindow() throws Exception{
            System.out.println("Start checking...");


            // Picture path file
            String outPath = firmtempFile;
            String inPath = flowtempFile;


            // Read input file
            allLinesList = readFileToList(inPath);
            // Output
            //addtoFile(outPath, "");

            // First peak time and anchor point
            peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
            // First peak coordinate
            peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);

            // Set stage title
            stage.setTitle("Seismic Phase Inspection Visualization Platform");
            // Stage size is variable
            stage.setResizable(false);
            // Stage style
            stage.initStyle(StageStyle.DECORATED);

            // First picture
            imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);



            // Define button
            Button button1 = new Button("Confirm");
            // Position
            //button1.setLayoutY(500);button1.setLayoutX(250);
            // Style
            button1.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: green; -fx-border-width: 3px ; -fx-border-radius:30");
            // Dimension
            button1.setPrefWidth(130);button1.setPrefHeight(80);
            // Font
            button1.setFont(Font.font("Times New Roman",FontWeight.BOLD,24));// Application system font, bold, size 30, as button font


            // Define button
            Button button2 = new Button("Undo");
            // Position
            //button2.setLayoutY(500);button2.setLayoutX(250);
            // Style
            button2.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: orange; -fx-border-width: 3px ; -fx-border-radius:30");
            // Dimension
            button2.setPrefWidth(130);button2.setPrefHeight(80);
            // Font
            button2.setFont(Font.font("Times New Roman",FontWeight.BOLD,28));// Application system font, bold, size 30, as button font


            // Define button
            Button button3 = new Button("Discard");
            // Position
            //label.setLayoutY(100);label.setLayoutX(200);
            // Style
            button3.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: red; -fx-border-width: 3px ; -fx-border-radius:30");
            // Dimension
            button3.setPrefWidth(130);button3.setPrefHeight(80);
            // Font
            button3.setFont(Font.font("Times New Roman",FontWeight.BOLD,24));// Application system font, bold, size 30, as button font


            // Define canvas
            Canvas canvas = new Canvas(CanvasWidth,CanvasHeight); // Canvas length and width

            // Define the brush
            GraphicsContext graphicscontext = canvas.getGraphicsContext2D();
            // Brush color
            graphicscontext.setStroke(Color.YELLOW);
            // Brush linearity
            graphicscontext.setLineWidth(3);

            //Developer mode
            if (DeveloperMode){
                graphicscontext.strokeLine(0,0,0,CanvasHeight);
                graphicscontext.strokeLine(CanvasWidth,0,CanvasWidth,CanvasHeight);
            }


            // First Pick-up
            if (historyLoad){
                // Draw history pick-up
                if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                    pick_index = peaksIdxHistoryList.get(index);
                    graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                    pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                }else{
                    pick_time = -999;
                }
            } else {
                // Draw the highest peak pick-up
                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
            }
            //System.out.println(pick_time);



            // Define button events      
            button1.setOnAction(event ->{
                if (index<allLinesList.length) {
                    //System.out.println(pick_time);
                    
                    // Write in file
                    // System.out.println(+allLinesList[index].indexOf("-9 -9 -9"));
                    int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                    String input = allLinesList[index].substring(0,i) + String.format("%9.3f", pick_time);
                    try {
                        writetoFile(outPath, input,true);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Picking up history
                    peaksIdxLogList.add(-1);
                    peaksIdxLogList.set(index,pick_index);
                    //System.out.println(peaksIdxLogList.get(index));

                    // Switch pictures
                    index++;
                    if (index<allLinesList.length){
                        //Clean up the mark
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        //System.out.println(index);
                        imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);

                        // Peak time and anchor point
                        peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                        // Peak coordinates
                        peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                        
                        if (historyLoad){
                            // Draw history pick-up
                            if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                                pick_index = peaksIdxHistoryList.get(index);
                                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            }else{
                                pick_index = peaksIdxHistoryList.get(index);
                                pick_time = -999;
                            }
                        } else {
                            // Draw the highest peak pickup
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        }
                        //System.out.println(pick_time);

                    } else {
                        // Ending flag
                        recoverSort = true;
                        // Exit reminder
                        event.consume();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Exit");
                        alert.setHeaderText(null);
                        alert.setContentText("You have completed all the checks, whether to exit ?\nJh.Xiang production, problem contact: xiangjinghong21@mails.ucas.ac.cn\n2023.2.14");
                        Optional<ButtonType> result = alert.showAndWait();
                        if(result.get() == ButtonType.OK){
                            Platform.exit(); 
                        }
                    }
                }               
            }); 

            // Define button event 
            button2.setOnAction(event -> {
                if (index>0) {

                    index --;

                    // Clean up the mark
                    graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                    // Peak time and anchor point
                    peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                    // Peak coordinates
                    peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                    if ( peaksIdxLogList.get(index)> -1 && peaksIdxLogList.get(index) < peaksIdxList.length ) {
                        // Read pick-up history
                        pick_index = peaksIdxLogList.get(index);
                        // Draw pick-up
                        graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                        pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        //System.out.println(pick_time);
                        // Write in file
                        int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                        String input = allLinesList[index].substring(0,i) +  String.format("%9.3f", pick_time) + "   False";
                        try {
                            writetoFile(outPath, input,true);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        pick_time = -999;
                        //System.out.println(pick_time);
                        // Write in file
                        int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                        String input = allLinesList[index].substring(0,i) +  String.format("%9.3f", pick_time) + "   False";
                        try {
                            writetoFile(outPath, input,true);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                
        
                    // Switch pictures
                    //System.out.println(index);
                    imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);


                }
            });

            // Define button event    
            button3.setOnAction(event -> {
                if (index<allLinesList.length) {
                    //System.out.println(pick_time);
                    pick_time = -999;
                    pick_index = -1;
                    
                    // Write in file
                    int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                    String input = allLinesList[index].substring(0,i) + String.format("%9.3f", pick_time);
                    try {
                        writetoFile(outPath, input,true);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Pick-up history
                    peaksIdxLogList.add(-1);
                    peaksIdxLogList.set(index,pick_index);
                    //System.out.println(peaksIdxLogList.get(index));

                    // Switch pictures
                    index++;
                    if (index<allLinesList.length){
                        //Clean up the mark
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        //System.out.println(index);
                        imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);

                        // Peak time and anchor point
                        peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                        // Peak coordinates
                        peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                        
                        if (historyLoad){
                            // Draw history pick-up
                            if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                                pick_index = peaksIdxHistoryList.get(index);
                                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            }else{
                                pick_index = peaksIdxHistoryList.get(index);
                                pick_time = -999;
                            }
                        } else {
                            // Draw the highest peak pick-up
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        }
                        //System.out.println(pick_time);

                    } else {
                        // Clean up the mark
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);
                        // ending flag
                        recoverSort = true;
                        // Exit reminder
                        event.consume();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Exit");
                        alert.setHeaderText(null);
                        alert.setContentText("You have completed all the checks, whether to exit ?\nJh.Xiang production, problem contact: xiangjinghong21@mails.ucas.ac.cn\n2023.2.14");
                        Optional<ButtonType> result = alert.showAndWait();
                        if(result.get() == ButtonType.OK){
                            Platform.exit(); 
                        }
                    }
                }               
            });
            
            // Define layout 
            HBox hbox = new HBox();              // Horizontal box layout
            BorderPane borderpane = new BorderPane();  // Upper and lower left and right middle layout
            AnchorPane anchorpane = new AnchorPane();// Anchor point layout
            hbox.setPadding(new Insets(10, 525, 65, 525)); // Distance from node to edge
            hbox.setSpacing(40); // Spacing between nodes

            AnchorPane.setLeftAnchor(imageview, 0.0);
            AnchorPane.setTopAnchor(imageview, 0.0);
            AnchorPane.setLeftAnchor(canvas, CanvasLeft);// Adjust the size of the positioning canvas position
            AnchorPane.setTopAnchor(canvas, 40.0);  // Adjust the size of the positioning canvas position

            // Layout interaction       Put object into layout
            borderpane.setBottom(hbox);
            borderpane.setTop(anchorpane);
            hbox.getChildren().addAll(button2,button1,button3);

            anchorpane.getChildren().addAll(imageview,canvas);
            // Create scene
            Scene scene =  new Scene(borderpane,1500,1000);//Put the layout into the scene and set the width and height
            
            // Define the roller event
            scene.setOnScroll(event -> {
                // System.out.println(event.getDeltaY());
                if (event.getDeltaY()>0){

                    if (index<allLinesList.length){
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        pick_index ++;

                        if (pick_index  == peaksIdxList.length){
                            // Label
                            //graphicscontext.strokeLine(CanvasWidth,0,CanvasWidth,CanvasHeight);
                            pick_time = -999;
                            //System.out.println(pick_time);
                        } else if (pick_index  == peaksIdxList.length +1 ){
                            // Label
                            pick_index = 0;
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        } else{
                            // Label
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        }

                    }       
                } else if (event.getDeltaY()<0){

                    if (index<allLinesList.length){

                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        pick_index --;

                        if (pick_index  == -1){
                            // Label
                            //graphicscontext.strokeLine(0,0,0,CanvasHeight);
                            pick_time = -999;
                            //System.out.println(pick_time);
                        } else if (pick_index  == -2 ){
                            // Label
                            pick_index = peaksIdxList.length - 1;
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        } else{
                            // Label
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        }
                    }
                }                
            });
            // Put scene into stage
            stage.setScene(scene);
            // Exit protection reminder
            Platform.setImplicitExit(false);
            stage.setOnCloseRequest(event -> {
                event.consume();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit");
                alert.setHeaderText(null);
                alert.setContentText("Do you want to end the check ?");
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK){
                    Platform.exit(); 
                }
            });
            // Show stage
            stage.show();
        }



	public void imagereveal(String path)  {
        //ImageView imageview = new ImageView();  
        Image image = new Image(new File(path).toURI().toString());
        imageview.setImage(image); // The picture is put into 'ImageView'
	}

    public float[] linesToPeaksList(String lines)  {

        // Split into lists
        String[] linelist =  lines.trim().split("\\s+");
        // Intercept the data list
        List<String> peak1 = new ArrayList<>();
        for (int j=22; j<linelist.length; j++){
            peak1.add(linelist[j]);
        }
        // Introduce the maximum peak index to 'pick_index'
        String[] peak2 = peak1.toArray(new String[peak1.size()]);
        pick_index = Integer.valueOf(peak2[0]);
        //System.out.println(pick_index);
        // Remove the first item (maximum peak index)
        float[] peak3 = new  float[peak2.length - 1];
        //System.out.println(peak2.length);
        for (int i=1,j=0; i<peak2.length; i++,j++) {

            peak3[j] = Float.valueOf(peak2[i]);
            // System.out.println("i:"+i);
            // System.out.println("j:"+j);
            // System.out.println(peak3[j]);
            // System.out.println(peak2[i]);

        }
        //System.out.println(peak3.length);
        return peak3;

	}

    public float[] PeaksTimeToIdxList(float[] times)  {

        // //The earthquake and the arrival time are different days
        // for (int i =1; i<times.length; i++){
        //     if (times[i]<times[0]){
        //         times[i] += 86400;
        //     }
        // }
        float[] peak_idx = new float[times.length - 2];
        //Factor calculation
        float factor = CanvasWidth/(times[1]-times[0]);
        //Remove the first two ( anchor point )
        //System.out.println(times.length);
        for (int i=2,j=0; i<times.length; i++,j++){
            peak_idx[j] = (times[i]-times[0])* factor;
            // System.out.println("i:"+i);
            // System.out.println("j:"+j);
            // System.out.println(peak_idx[j]);
        }
        //System.out.println(peak_idx.length);

        return peak_idx;

    }
    
    public static void writetoFile(String filepath,String content,Boolean bool) throws IOException{
        FileWriter fWriter=new FileWriter(filepath,bool);
        fWriter.write(content+ "\n" );
        fWriter.flush();
        fWriter.close();
    }

	public static String[] readFileToList(String filepath) throws Exception{
		List<String> allLines = Files.readAllLines(Paths.get(filepath));
		String[] allLineslist = allLines.toArray(new String[allLines.size()]);
		return allLineslist;
	}

    //Rewrite class  Generally used for clean-up work
    @Override
    public void stop() throws IOException {
        if((new File(firmtempFile)).exists())  {
            System.out.println("Post-processing...");
            // Remove temporary data
            TempRemove();

            //Pick to modify when it arrives
            ArrivalTimeModify();

            // Stations number update
            StationNumUpdate();

            // Recovery sort
            if (epiModel == true && recoverSort == true){
                RecoverTimeSort();
            } 
        } 
        // Display current time
        PrintTime("End");
    }

    public static  void  TempRemove() throws IOException 
    {
        List<String> allLines = Files.readAllLines(Paths.get(firmtempFile));
        LinkedHashMap<String, String> rays = new LinkedHashMap<>();

        for (String lines : allLines)
        {
            String ray = lines.substring(0,38);
            String info = lines.substring(38);
            rays.put(ray,info);
            
        }

        // Define output file path
        FileWriter fruitf = new FileWriter(outFile);
        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();
        for (Map.Entry<String, String> entry : rays.entrySet())
        {
            String output = entry.getKey() + entry.getValue();
            fruit.append(output).append(line);
        }

        fruitf.write(fruit.toString());
        fruitf.close();

        System.out.println("Temporary data has been removed");
    }

    public static  void  ArrivalTimeModify() throws IOException 
    {
        List<String> allLines = Files.readAllLines(Paths.get(outFile));
        // Define output file path
        FileWriter fruitf = new FileWriter(outFile);
        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();

        for (String lines : allLines)
        {
            String front = lines.substring(0,71);
            String behind = lines.substring(80,137);
            String newArrival = lines.substring(139,148);
            // System.out.println(front.length());
            // System.out.println(behind.length());
            // System.out.println(newArrival.length());
            if (! newArrival.trim().equals("-999.000") )  {
                String output = front + newArrival + behind;
                fruit.append(output).append(line);
            }
        }
        
        fruitf.write(fruit.toString());
        fruitf.close();

        System.out.println("Pick-up travel-time has been modified");

    }

    public static  void  StationNumUpdate() throws IOException 
    {
        List<String> allLines = Files.readAllLines(Paths.get(outFile));
        HashMap<String, Integer> events = new HashMap<>();

        for (String lines : allLines)
        {
            String lineevent = lines.substring(0,19);
            events.put(lineevent,0);
        }

        for (String lines : allLines)
        {
            for (Map.Entry<String, Integer> entry : events.entrySet())
            {
                if (lines.startsWith(entry.getKey()))
                {
                    events.put(entry.getKey(),entry.getValue()+1);
                }
            }
        }

        // Define output file path
        FileWriter fruitf = new FileWriter(outFile);
        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();

        for (String lines : allLines)
        {
            String output = lines.substring(0,97)+String.format("%3d",events.get(lines.substring(0,19)))+lines.substring(100);
            fruit.append(output).append(line);
        }

        fruitf.write(fruit.toString());
        fruitf.close();

        System.out.println("Stations number has been updated");
    }

    public static  void  RecoverTimeSort() throws IOException 
    {
        System.out.println("Restoring time sort...");
        
        List<String> allLines = Files.readAllLines(Paths.get(outFile));
        List<String> allLinesrefer = Files.readAllLines(Paths.get(rawFile));
        
        // Define output file path
        FileWriter fruitf = new FileWriter(outFile);
        String line = System.getProperty("line.separator");
        StringBuffer fruit = new StringBuffer();

        long begin=System.currentTimeMillis();
        for (String linesrefer : allLinesrefer)
        {
            for (String lines : allLines )
            {
                String rayrefer = linesrefer.substring(0,38);
                String ray = lines.substring(0,38);
                if (ray.equals(rayrefer)){
                    fruit.append(lines).append(line);
                }
            }
        }
        long end=System.currentTimeMillis();

        fruitf.write(fruit.toString());
        fruitf.close();
        System.out.println("Time sort has been restored, time (s): " + (end - begin)/1000);
    }
    
    public static void main(String[] args) throws IOException {
        
        // Read parameters of command line (configuration file path)
        configsdir = args[0];

        // Invoke 'launch' method to automatically invoke 'init ( )', 'start ( )', 'stop ( )' methods.
        Application.launch();
    }

}
