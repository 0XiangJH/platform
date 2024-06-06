
/*
名称：震相检查可视化平台 （Seismic Phase Inspection Visualization Platform）
功能：对自动拾取结果进行检查
时间：2022.12.1
设计：向景鸿

日志：2022.12.21  --  设置丢弃、保留功能，自动生成成像格式
     2023.1.7    --  增加撤销、调整功能，完善丢弃、保留功能
     2023.1.9    --  增加滚轮调整功能
     2023.1.11   --  增加记忆功能
     2023.2.1    --  增加震中距模式
     2023.2.2    --  增加预览功能
     2023.2.11   --  增加检查历史显示功能
     2023.2.12   --  界面美化    
     2023.3.27   --  增加免检功能
     2023.3.27   --  完善震中距功能
     2023.4.28   --  增加Sn震相，并增加后续震相扩展工具
     2023.6.21   --  增加主页，扩展提前配置参数功能
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
    //Application是JavaFX程序的入口，任何JavaFX程序都要继承该类并重写start()等方法
    
    // 震相类型
    static Integer phaseType ;
    static int phaseTypeInt;
    static String phaseTypeString;

    // 筛选级别
    static String level ;
    // 最小震中距
    static float minGcarc ;
    // 最大震中距
    static float maxGcarc ;

    // 图片文件路径
	static String imagedir0;
    // 文本文件路径
    static String inFile0;
    // 输出路径
    static String outFile0 ;

    // 开启震中距模式，默认时间模式
    static Boolean epiModel ;
    //是否开启顺序恢复功能，默认关闭(完成所有筛选后自启)
    static Boolean recoverSort;

    //加载检查历史
    static Boolean historyLoad ;
    //拾取检查文件
    static String historyFile ;

    // 加载免检功能
    static Boolean exemptionLoad;
    // 免检文件
    static String exemptionFile;
    
    // 详细文件
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
            //读取配置文件
            Properties prop = new Properties();
            //获取读取properties文件的输入流对象
            InputStream is =Check.class.getClassLoader().getResourceAsStream(configsdir);
            //通过给定的输入流对象读取properties文件并解析。
            prop.load(is);
            //获取properties文件中的内容
    
            //震相类型
            phaseType = Integer.valueOf(prop.getProperty("phaseType"));
            //筛选级别
            level = prop.getProperty("level");
            //最小震中距
            minGcarc = Float.parseFloat(prop.getProperty("minGcarc"));
            //最大震中距
            maxGcarc = Float.parseFloat(prop.getProperty("maxGcarc"));
    
            //图片文件路径
            imagedir0 = prop.getProperty("imagedir");
            //文本文件路径
            inFile0 = prop.getProperty("inFile");
            //输出路径
            outFile0 = prop.getProperty("outFile");
    
            //是否开启震中距模式
            epiModel = Boolean.valueOf(prop.getProperty("epiModel"));
            //是否开启顺序恢复功能
            recoverSort = Boolean.valueOf(prop.getProperty("recoverSort"));
    
            //是否加载检查历史
            historyLoad=Boolean.valueOf(prop.getProperty("historyLoad"));
            //检查历史文件路径
            historyFile=prop.getProperty("historyFile");
    
            // 是否开启免检功能
            exemptionLoad=Boolean.valueOf(prop.getProperty("exemptionLoad"));
            // 免检文件路径
            exemptionFile=prop.getProperty("exemptionFile");
    
            // //输入输出文件路径
            // rawFile = prop.getProperty("rawFile");
            // outFile = prop.getProperty("outFile");
            // //临时文件路径
            // flowtempFile = prop.getProperty("flowtempFile");
            // firmtempFile = prop.getProperty("firmtempFile");
            // rawsorttempFile = prop.getProperty("rawsorttempFile");
    
        } catch (Exception e) {
            System.out.println("读取配置文件失败！");
        }
    }

    public static void Pretreat() throws IOException{
        // 震相判断
        try {
            PhaseJudge();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 显示当前时间
        try {
            PrintTime("开始");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("预处理...");
        
        // 查明进度
        try {
            FindProgress();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 初筛选
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

        // 检查历史读取
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
            System.out.println(phaseTypeString+"震相检查");
            break;
            case 2 :
            phaseTypeString = "Sn";
            CanvasWidth = 1309;CanvasLeft= 112.0;
            System.out.println(phaseTypeString+"震相检查");
            break;
            default :
            phaseTypeString = "";
            System.out.println("震相输入错误，请检查配置文件！");
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
        System.out.println("读取免检文件...");
        File file=new File(exemptionFile);  
        if(!file.exists()){
            exemptionLoad = false;
            System.out.println("免检文件不存在!");
        } else {
            long begin=System.currentTimeMillis();
            String inputFile = rawFile;
            String outputFile = flowtempFile;
            List<String> allLines = Files.readAllLines(Paths.get(inputFile));
            List<String> allLinesExemption = Files.readAllLines(Paths.get(exemptionFile));
            //定义输出文件路径
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
            System.out.println("免检文件读取完毕, 用时(s): "+ (end - begin)/1000);
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
                System.out.println("从该处继续检查: " + nowProgress);
            } else {
                nowProgress = "";
                System.out.println("重头开始检查");
            }
        } else {
            nowProgress = "";
            System.out.println("重头开始检查");
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

            ////筛选并统计
            //字符串转换为列表
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

            System.out.println("统计: A/B/C/D   分别计([" + minGcarc + ","+maxGcarc+")): "+ a +"("+ aarc +") / " + b +"("+ barc +") / "+ c +"("+ carc +") / "+ d+"("+ darc +")");
            System.out.println("原射线数([" + minGcarc + ","+maxGcarc+")): "+ sum+"("+ sumarc +")");
            System.out.println("筛选出: "+ level+" and " + "[" + minGcarc + ","+maxGcarc+")" + "   射线数: "+ outsum);
            ////排序
            String[] allLineslist = allLinesscreen.toArray(new String[allLinesscreen.size()]);

            //选择排序算法
            System.out.println("新的排序...");
            long begin=System.currentTimeMillis();
            for (int i = 0; i < allLineslist.length; i++) {
                int minIndex = i;
                // 把当前遍历的数和后面所有的数进行比较，并记录下最小的数的下标
                for (int j = i + 1; j < allLineslist.length; j++) {
                    if (Float.valueOf(allLineslist[j].trim().split("\\s+")[5]) < Float.valueOf(allLineslist[minIndex].trim().split("\\s+")[5])) 
                    {
                        // 记录最小的数的下标
                        minIndex = j;
                    }
                }
                // 如果最小的数和当前遍历的下标不一致，则交换
                if (i != minIndex) {
                    String temp = allLineslist[i];
                    allLineslist[i] = allLineslist[minIndex];
                    allLineslist[minIndex] = temp;
                }
            }
            long end=System.currentTimeMillis();

            List<String> allLinesArrays = Arrays.asList(allLineslist);

            //定义输出文件路径
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

            System.out.println("按震中距排序已完成，用时(s): " + (end - begin)/1000);

        } else if (rawsortFile0.exists()) {
            System.out.println("按已有排序");
        }

        ////恢复记忆并统计
        String inputFile = rawsorttempFile;
        String outputFile = flowtempFile;
        List<String> allLines = Files.readAllLines(Paths.get(inputFile));
        //定义输出文件路径
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
        System.out.println("剩余射线数: "+ outsum);
        System.out.println("预览:  "+ region1[0]+":"+a + "   " + region1[1] +":"+b + "   " + region1[2] +":"+c + "   " + region1[3] +":"+d );
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

        //字符串转换为列表
        String[] levellist = level.split("");
        //System.out.println(levellist[1]);
        List<String> allLines = Files.readAllLines(Paths.get(inputFile));
        //定义输出文件路径
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
        System.out.println("统计: A/B/C/D   分别计([" + minGcarc + ","+maxGcarc+")): "+ a +"("+ aarc +") / " + b +"("+ barc +") / "+ c +"("+ carc +") / "+ d+"("+ darc +")");
        System.out.println("原射线数([" + minGcarc + ","+maxGcarc+")): "+ sum+"("+ sumarc +")");
        System.out.println("筛选出: "+ level+" and " + "[" + minGcarc + ","+maxGcarc+")" + "   射线数: "+ outsum);
    }

    public static void HistoryRead() throws IOException
    {
        System.out.println("读取检查历史文件...");
        File file=new File(historyFile);  
        if(!file.exists()){
            historyLoad = false;
            System.out.println("检查历史文件不存在!");
        } else {
            long begin=System.currentTimeMillis();
            String inputFile = flowtempFile;
            List<String> allLines = Files.readAllLines(Paths.get(inputFile));
            List<String> allLinesHistory = Files.readAllLines(Paths.get(historyFile));
            // 遍历所有流动临时文件
            for (int i=0; i<allLines.size(); i++){
                // 分割为列表
                String[] linelist =  allLines.get(i).trim().split("\\s+");
                // 截取到时列表
                List<String> peak1 = new ArrayList<>();
                for (int j=25; j<linelist.length; j++){
                    peak1.add(linelist[j]);
                }
                String[] peak2 = peak1.toArray(new String[peak1.size()]);
                
                peaksIdxHistoryList.add(peak2.length);
                // 遍历所有拾取历史
                for (String linesHistory : allLinesHistory){
                    String[] lineHistorylist =  linesHistory.trim().split("\\s+");
                    String rayHistory = String.join("  ", lineHistorylist[0], lineHistorylist[1], lineHistorylist[2], lineHistorylist[3]);
                    String ray = String.join("  ", linelist[0], linelist[1], linelist[2], linelist[3]);
                    // 找出存在的记录
                    if (ray.equals(rayHistory)){
                        String arrivalHistory = lineHistorylist[8];
                        // System.out.println(arrivalHistory);
                        // 遍历所有可能到时
                        for (int k=0; k<peak2.length; k++){
                            String arrival = peak2[k];
                            // 找出相同的到时
                            if (arrival.equals(arrivalHistory)){
                                // 写入到时索引
                                peaksIdxHistoryList.set(i,k);
                            }
                        }
                    }
                }
                //System.out.println(peaksIdxHistoryList.get(i));
            }
            long end=System.currentTimeMillis();
            System.out.println("检查历史文件读取完毕, 用时(s): "+ (end - begin)/1000);
            //System.out.println(peaksIdxHistoryList.size());
        }
    }

    //重写类  一般用于初始化工作
    @Override
    public void init() throws Exception {
        System.out.println("请在确认配置后, 点击开始键继续...");
    }

    //重写类
    @Override
    public void start(Stage stage) throws Exception {            //参数是舞台实例
        //舞台大小可变
        stage.setResizable(false);
        //舞台样式
        stage.initStyle(StageStyle.DECORATED);
        
        //定义按钮
        Button button_begin1 = new Button("开始");
        //位置
        //button1.setLayoutY(500);button1.setLayoutX(250);
        //样式
        button_begin1.setStyle("-fx-background-color: white; -fx-background-radius:10; -fx-border-color: black; -fx-border-width: 1px ; -fx-border-radius:10");
        //尺寸
        button_begin1.setPrefWidth(90);button_begin1.setPrefHeight(70);
        //字体
        button_begin1.setFont(Font.font("宋体",FontWeight.BOLD,25));//应用系统字体，粗体，30号大小，作为按钮字体
        //光标
        button_begin1.setCursor(Cursor.CLOSED_HAND);

        //定义按钮
        Button button_begin2 = new Button("配置");
        //位置
        //button1.setLayoutY(500);button1.setLayoutX(250);
        //样式
        button_begin2.setStyle("-fx-background-color: white; -fx-background-radius:1; -fx-border-color: black; -fx-border-width: 1px ; -fx-border-radius:10");
        //尺寸
        button_begin2.setPrefWidth(90);button_begin2.setPrefHeight(70);
        //字体
        button_begin2.setFont(Font.font("宋体",FontWeight.BOLD,25));//应用系统字体，粗体，30号大小，作为按钮字体
        //光标
        button_begin2.setCursor(Cursor.CLOSED_HAND);

        //定义标签
        Label label = new Label("欢迎使用震相检查可视化平台");
        label.setFont(Font.font("宋体",FontWeight.BOLD,55));
        
        //定义开始布局
        HBox hbox0 = new HBox();              //水平盒子布局
        hbox0.setPadding(new Insets(10, 340, 65, 340)); //节点到边缘的距离
        hbox0.setSpacing(40); //节点之间的间距
        BorderPane borderpane0 = new BorderPane();  //上下左右中布局
        borderpane0.setBottom(hbox0);
        hbox0.getChildren().addAll(button_begin1, button_begin2);
        borderpane0.setBottom(hbox0);
        borderpane0.setCenter(label);

        //创建场景
        Scene beginscene =  new Scene(borderpane0,900,600);//将布局放进场景里面,并设置宽度高度

        //把场景放进舞台里面
        stage.setScene(beginscene);

        //定义开始事件       
        button_begin1.setOnAction(event ->{

            // 读取配置文件
            try {
                ConfigsRead();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // 预处理
            try {
                Pretreat();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            stage.hide();

            // 子窗口
            try {
                Subwindow();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }); 

        //定义配置事件       
        button_begin2.setOnAction(event ->{
            try {
                String file = configsdir; // 文件路径
                Process p = Runtime.getRuntime().exec("gedit " + file); // 执行打开文件的命令行
                p.waitFor(); // 等待程序执行完毕
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }); 

        //退出保护提醒
        Platform.setImplicitExit(false);
        stage.setOnCloseRequest(event -> {
            event.consume();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出程序");
            alert.setHeaderText(null);
            alert.setContentText("您是否要结束检查？");
            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == ButtonType.OK){
                Platform.exit(); 
            }
        });
        //显示舞台
        stage.show();
    }

        private final Stage stage = new Stage();
        public void Subwindow() throws Exception{
            System.out.println("开始检查...");


            // 图片路径文件
            String outPath = firmtempFile;
            String inPath = flowtempFile;


            // 读取输入文件
            allLinesList = readFileToList(inPath);
            // 输出
            //addtoFile(outPath, "");

            // 首项峰时间及锚点
            peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
            // 首项峰坐标
            peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);

            //设置舞台标题
            stage.setTitle("震相检查可视化平台");
            //舞台大小可变
            stage.setResizable(false);
            //舞台样式
            stage.initStyle(StageStyle.DECORATED);

            //首项图片
            imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);



            //定义按钮
            Button button1 = new Button("确认");
            //位置
            //button1.setLayoutY(500);button1.setLayoutX(250);
            //样式
            button1.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: green; -fx-border-width: 3px ; -fx-border-radius:30");
            //尺寸
            button1.setPrefWidth(130);button1.setPrefHeight(80);
            //字体
            button1.setFont(Font.font("宋体",FontWeight.BOLD,35));//应用系统字体，粗体，30号大小，作为按钮字体


            //定义按钮
            Button button2 = new Button("撤销");
            //位置
            //button2.setLayoutY(500);button2.setLayoutX(250);
            //样式
            button2.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: orange; -fx-border-width: 3px ; -fx-border-radius:30");
            //尺寸
            button2.setPrefWidth(130);button2.setPrefHeight(80);
            //字体
            button2.setFont(Font.font("宋体",FontWeight.BOLD,35));//应用系统字体，粗体，30号大小，作为按钮字体


            //定义按钮
            Button button3 = new Button("丢弃");
            //位置
            //label.setLayoutY(100);label.setLayoutX(200);
            //样式
            button3.setStyle("-fx-background-color: white; -fx-background-radius:30; -fx-border-color: red; -fx-border-width: 3px ; -fx-border-radius:30");
            //尺寸
            button3.setPrefWidth(130);button3.setPrefHeight(80);
            //字体
            button3.setFont(Font.font("宋体",FontWeight.BOLD,35));//应用系统字体，粗体，30号大小，作为按钮字体


            //定义画布
            Canvas canvas = new Canvas(CanvasWidth,CanvasHeight); //画布长宽

            //定义画笔
            GraphicsContext graphicscontext = canvas.getGraphicsContext2D();
            //画笔颜色
            graphicscontext.setStroke(Color.YELLOW);
            //画笔线性
            graphicscontext.setLineWidth(3);

            //开发者模式
            if (DeveloperMode){
                graphicscontext.strokeLine(0,0,0,CanvasHeight);
                graphicscontext.strokeLine(CanvasWidth,0,CanvasWidth,CanvasHeight);
            }


            // 首项拾取
            if (historyLoad){
                // 绘制历史拾取
                if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                    pick_index = peaksIdxHistoryList.get(index);
                    graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                    pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                }else{
                    pick_time = -999;
                }
            } else {
                // 绘制最高峰拾取
                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
            }
            //System.out.println(pick_time);



            //定义按钮事件       
            button1.setOnAction(event ->{
                if (index<allLinesList.length) {
                    //System.out.println(pick_time);
                    
                    // 写入文件
                    // System.out.println(+allLinesList[index].indexOf("-9 -9 -9"));
                    int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                    String input = allLinesList[index].substring(0,i) + String.format("%9.3f", pick_time);
                    try {
                        writetoFile(outPath, input,true);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // 拾取历史
                    peaksIdxLogList.add(-1);
                    peaksIdxLogList.set(index,pick_index);
                    //System.out.println(peaksIdxLogList.get(index));

                    // 切换图片
                    index++;
                    if (index<allLinesList.length){
                        //清理标记
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        //System.out.println(index);
                        imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);

                        // 峰时间及锚点
                        peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                        // 峰坐标
                        peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                        
                        if (historyLoad){
                            // 绘制历史拾取
                            if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                                pick_index = peaksIdxHistoryList.get(index);
                                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            }else{
                                pick_index = peaksIdxHistoryList.get(index);
                                pick_time = -999;
                            }
                        } else {
                            // 绘制最高峰拾取
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        }
                        //System.out.println(pick_time);

                    } else {
                        // 结束标志
                        recoverSort = true;
                        // 退出提醒
                        event.consume();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("退出程序");
                        alert.setHeaderText(null);
                        alert.setContentText("您已完成所有检查,是否要退出程序？\n向景鸿制作, 有问题联系xiangjinghong21@mails.ucas.ac.cn\n2023.2.14");
                        Optional<ButtonType> result = alert.showAndWait();
                        if(result.get() == ButtonType.OK){
                            Platform.exit(); 
                        }
                    }
                }               
            }); 

            //定义按钮事件      
            button2.setOnAction(event -> {
                if (index>0) {

                    index --;

                    //清理标记
                    graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                    // 峰时间及锚点
                    peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                    // 峰坐标
                    peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                    if ( peaksIdxLogList.get(index)> -1 && peaksIdxLogList.get(index) < peaksIdxList.length ) {
                        // 读取拾取历史
                        pick_index = peaksIdxLogList.get(index);
                        // 绘制拾取
                        graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                        pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        //System.out.println(pick_time);
                        // 写入文件
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
                        // 写入文件
                        int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                        String input = allLinesList[index].substring(0,i) +  String.format("%9.3f", pick_time) + "   False";
                        try {
                            writetoFile(outPath, input,true);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                
        
                    // 切换图片
                    //System.out.println(index);
                    imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);


                }
            });

            //定义按钮事件      
            button3.setOnAction(event -> {
                if (index<allLinesList.length) {
                    //System.out.println(pick_time);
                    pick_time = -999;
                    pick_index = -1;
                    
                    // 写入文件
                    int i = allLinesList[index].indexOf("-9 -9 -9") +10;
                    String input = allLinesList[index].substring(0,i) + String.format("%9.3f", pick_time);
                    try {
                        writetoFile(outPath, input,true);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // 拾取历史
                    peaksIdxLogList.add(-1);
                    peaksIdxLogList.set(index,pick_index);
                    //System.out.println(peaksIdxLogList.get(index));

                    // 切换图片
                    index++;
                    if (index<allLinesList.length){
                        //清理标记
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        //System.out.println(index);
                        imagereveal(imagedir+allLinesList[index].trim().split("\\s+")[21]);

                        // 峰时间及锚点
                        peaksTimeAndAnchorList = linesToPeaksList(allLinesList[index]);
                        // 峰坐标
                        peaksIdxList = PeaksTimeToIdxList(peaksTimeAndAnchorList);
                        
                        if (historyLoad){
                            // 绘制历史拾取
                            if (peaksIdxHistoryList.get(index)<peaksIdxList.length){
                                pick_index = peaksIdxHistoryList.get(index);
                                graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                                pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            }else{
                                pick_index = peaksIdxHistoryList.get(index);
                                pick_time = -999;
                            }
                        } else {
                            // 绘制最高峰拾取
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                        }
                        //System.out.println(pick_time);

                    } else {
                        //清理标记
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);
                        // 结束标志
                        recoverSort = true;
                        // 退出提醒
                        event.consume();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("退出程序");
                        alert.setHeaderText(null);
                        alert.setContentText("您已完成所有检查,是否要退出程序？\n向景鸿制作, 有问题联系xiangjinghong21@mails.ucas.ac.cn\n2023.2.14");
                        Optional<ButtonType> result = alert.showAndWait();
                        if(result.get() == ButtonType.OK){
                            Platform.exit(); 
                        }
                    }
                }               
            });
            
            //定义布局 
            HBox hbox = new HBox();              //水平盒子布局
            BorderPane borderpane = new BorderPane();  //上下左右中布局
            AnchorPane anchorpane = new AnchorPane();//锚点布局
            hbox.setPadding(new Insets(10, 525, 65, 525)); //节点到边缘的距离
            hbox.setSpacing(40); //节点之间的间距

            AnchorPane.setLeftAnchor(imageview, 0.0);
            AnchorPane.setTopAnchor(imageview, 0.0);
            AnchorPane.setLeftAnchor(canvas, CanvasLeft);//调节定位画布大小位置
            AnchorPane.setTopAnchor(canvas, 40.0);  //调节定位画布大小位置

            //布局交互       将对象放进布局里面
            borderpane.setBottom(hbox);
            borderpane.setTop(anchorpane);
            hbox.getChildren().addAll(button2,button1,button3);

            anchorpane.getChildren().addAll(imageview,canvas);
            //创建场景
            Scene scene =  new Scene(borderpane,1500,1000);//将布局放进场景里面,并设置宽度高度
            
            //定义滚轮事件
            scene.setOnScroll(event -> {
                // System.out.println(event.getDeltaY());
                if (event.getDeltaY()>0){

                    if (index<allLinesList.length){
                        graphicscontext.clearRect(0,0,CanvasWidth,CanvasHeight);

                        pick_index ++;

                        if (pick_index  == peaksIdxList.length){
                            // 标记
                            //graphicscontext.strokeLine(CanvasWidth,0,CanvasWidth,CanvasHeight);
                            pick_time = -999;
                            //System.out.println(pick_time);
                        } else if (pick_index  == peaksIdxList.length +1 ){
                            // 标记
                            pick_index = 0;
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        } else{
                            // 标记
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
                            // 标记
                            //graphicscontext.strokeLine(0,0,0,CanvasHeight);
                            pick_time = -999;
                            //System.out.println(pick_time);
                        } else if (pick_index  == -2 ){
                            // 标记
                            pick_index = peaksIdxList.length - 1;
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        } else{
                            // 标记
                            graphicscontext.strokeLine(peaksIdxList[pick_index],0,peaksIdxList[pick_index],CanvasHeight);
                            pick_time = peaksTimeAndAnchorList[ pick_index + 2];
                            //System.out.println(pick_time);
                        }
                    }
                }                
            });
            //把场景放进舞台里面
            stage.setScene(scene);
            //退出保护提醒
            Platform.setImplicitExit(false);
            stage.setOnCloseRequest(event -> {
                event.consume();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("退出程序");
                alert.setHeaderText(null);
                alert.setContentText("您是否要结束检查？");
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK){
                    Platform.exit(); 
                }
            });
            //显示舞台
            stage.show();
        }



	public void imagereveal(String path)  {
        //ImageView imageview = new ImageView();  
        Image image = new Image(new File(path).toURI().toString());
        imageview.setImage(image); //图片放入ImageView
	}

    public float[] linesToPeaksList(String lines)  {

        // 分割为列表
        String[] linelist =  lines.trim().split("\\s+");
        // 截取数据列表
        List<String> peak1 = new ArrayList<>();
        for (int j=22; j<linelist.length; j++){
            peak1.add(linelist[j]);
        }
        //引入最大峰索引到pick_index
        String[] peak2 = peak1.toArray(new String[peak1.size()]);
        pick_index = Integer.valueOf(peak2[0]);
        //System.out.println(pick_index);
        //去除第一项（最大峰索引）
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

        // //发震与到时不同天
        // for (int i =1; i<times.length; i++){
        //     if (times[i]<times[0]){
        //         times[i] += 86400;
        //     }
        // }
        float[] peak_idx = new float[times.length - 2];
        //因子计算
        float factor = CanvasWidth/(times[1]-times[0]);
        //去除前两项（锚点）
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

    //重写类  一般用于清理工作
    @Override
    public void stop() throws IOException {
        if((new File(firmtempFile)).exists())  {
            System.out.println("后处理...");
            //去除临时数据
            TempRemove();

            //拾取到时修改
            ArrivalTimeModify();

            //台站数更新
            StationNumUpdate();

            //恢复排序
            if (epiModel == true && recoverSort == true){
                RecoverTimeSort();
            } 
        } 
        // 显示当前时间
        PrintTime("结束");
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

        //定义输出文件路径
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

        System.out.println("临时数据已去除");
    }

    public static  void  ArrivalTimeModify() throws IOException 
    {
        List<String> allLines = Files.readAllLines(Paths.get(outFile));
        //定义输出文件路径
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

        System.out.println("拾取到时已修改");

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

        //定义输出文件路径
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

        System.out.println("台站数已更新");
    }

    public static  void  RecoverTimeSort() throws IOException 
    {
        System.out.println("恢复时间排序中...");
        
        List<String> allLines = Files.readAllLines(Paths.get(outFile));
        List<String> allLinesrefer = Files.readAllLines(Paths.get(rawFile));
        
        //定义输出文件路径
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
        System.out.println("时间排序已恢复，用时(s): " + (end - begin)/1000);
    }
    
    public static void main(String[] args) throws IOException {
        
        //读取命令行参数(配置文件路径)
        configsdir = args[0];

        //调用launch方法以先后自动调用init()、start()、stop()方法
        Application.launch();
    }

}
