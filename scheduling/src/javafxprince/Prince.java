package javafxprince;



import javafx.application.Application;

import javafx.application.Platform;

import javafx.geometry.Insets;

import javafx.scene.Scene;

import javafx.scene.chart.BarChart;

import javafx.scene.chart.CategoryAxis;

import javafx.scene.chart.NumberAxis;

import javafx.scene.chart.XYChart;

import javafx.scene.control.*;

import javafx.scene.layout.*;

import javafx.stage.Stage;

import javafx.concurrent.Task;



import java.sql.*;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedList;

import java.util.List;

import java.util.Queue;



public class Prince extends Application {

    private Database db = new Database();

    private TextArea outputArea;

    private Button connectButton;

    private Button fcfsButton;

    private Button sjfButton;

    private Button rrButton;



    // BarChart components

    private BarChart<String, Number> barChart;

    private XYChart.Series<String, Number> waitingTimeBarSeries;

    private XYChart.Series<String, Number> turnaroundTimeBarSeries;



    @Override

    public void start(Stage primaryStage) {

        primaryStage.setTitle("Scheduling Algorithms by Prince Roy");



        // UI Components

        connectButton = new Button("Connect to Database");

        fcfsButton = new Button("Run FCFS Scheduling");

        sjfButton = new Button("Run SJF Scheduling");

        rrButton = new Button("Run Round Robin Scheduling");



        outputArea = new TextArea();

        outputArea.setEditable(false);



        // Set up BarChart

        CategoryAxis xAxis = new CategoryAxis();

        xAxis.setLabel("Algorithm");



        NumberAxis yAxis = new NumberAxis();

        yAxis.setLabel("Average Time");



        barChart = new BarChart<>(xAxis, yAxis);

        barChart.setTitle("Average Waiting and Turnaround Times");



        waitingTimeBarSeries = new XYChart.Series<>();

        waitingTimeBarSeries.setName("Average Waiting Time");



        turnaroundTimeBarSeries = new XYChart.Series<>();

        turnaroundTimeBarSeries.setName("Average Turnaround Time");



        barChart.getData().addAll(waitingTimeBarSeries, turnaroundTimeBarSeries);



        // Set up button actions

        connectButton.setOnAction(e -> connectToDatabase());

        fcfsButton.setOnAction(e -> runFCFSScheduling());

        sjfButton.setOnAction(e -> runSJFScheduling());

        rrButton.setOnAction(e -> runRRScheduling());



        // Layout

        VBox layout = new VBox(10);

        layout.setPadding(new Insets(10));

        layout.getChildren().addAll(connectButton, fcfsButton, sjfButton, rrButton, new Label("Output:"), outputArea, barChart);



        // Scene

        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);

        primaryStage.show();

    }



    private void connectToDatabase() {

        try (Connection conn = db.connect()) {

            if (conn != null) {

                outputArea.appendText("Connected to database successfully.\n");

            }

        } catch (SQLException e) {

            outputArea.appendText("Database connection failed: " + e.getMessage() + "\n");

        }

    }



    private void runFCFSScheduling() {

        Task<Void> task = new Task<>() {

            @Override

            protected Void call() {

                Platform.runLater(() -> outputArea.appendText("Starting FCFS Scheduling...\n"));

                List<Process> processes = getSampleProcesses();

                new FCFS().schedule(processes, db, Prince.this);

                return null;

            }

        };

        new Thread(task).start();

    }



    private void runSJFScheduling() {

        Task<Void> task = new Task<>() {

            @Override

            protected Void call() {

                Platform.runLater(() -> outputArea.appendText("Starting SJF Scheduling...\n"));

                List<Process> processes = getSampleProcesses();

                new SJF().schedule(processes, db, Prince.this);

                return null;

            }

        };

        new Thread(task).start();

    }



    private void runRRScheduling() {

        Task<Void> task = new Task<>() {

            @Override

            protected Void call() {

                Platform.runLater(() -> outputArea.appendText("Starting Round Robin Scheduling with quantum 2...\n"));

                List<Process> processes = getSampleProcesses();

                new RoundRobin().schedule(processes, 2, db, Prince.this);

                return null;

            }

        };

        new Thread(task).start();

    }



    private List<Process> getSampleProcesses() {

        List<Process> processes = new ArrayList<>();

        processes.add(new Process(1, 0, 5));

        processes.add(new Process(2, 2, 3));

        processes.add(new Process(3, 4, 1));

        return processes;

    }



    public void updateChart(String algorithm, double avgWaitingTime, double avgTurnaroundTime) {

        Platform.runLater(() -> {

            waitingTimeBarSeries.getData().add(new XYChart.Data<>(algorithm, avgWaitingTime));

            turnaroundTimeBarSeries.getData().add(new XYChart.Data<>(algorithm, avgTurnaroundTime));

        });

    }



    public static void main(String[] args) {

        launch(args);

    }

}



class Process {

    public int processId;

    public int arrivalTime;

    public int burstTime;

    public int remainingTime;

    public int waitingTime;

    public int turnaroundTime;

    public int startTime;

    public int endTime;



    public Process(int id, int arrival, int burst) {

        this.processId = id;

        this.arrivalTime = arrival;

        this.burstTime = burst;

        this.remainingTime = burst;

    }

}



class Database {

    public Connection connect() throws SQLException {

        return DriverManager.getConnection("jdbc:mysql://localhost/prince", "root", ""); // Change password

    }



    public void insertResult(int processId, String algorithm, int arrivalTime, int burstTime, int startTime, int endTime, int waitingTime, int turnaroundTime) {

        String query = "INSERT INTO scheduling_results (process_id, algorithm, arrival_time, burst_time, start_time, end_time, waiting_time, turnaround_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, processId);

            pstmt.setString(2, algorithm);

            pstmt.setInt(3, arrivalTime);

            pstmt.setInt(4, burstTime);

            pstmt.setInt(5, startTime);

            pstmt.setInt(6, endTime);

            pstmt.setInt(7, waitingTime);

            pstmt.setInt(8, turnaroundTime);

            pstmt.executeUpdate();

        } catch (SQLException e) {

            System.out.println(e.getMessage());

        }

    }

}



class FCFS {

    public void schedule(List<Process> processes, Database db, Prince main) {

        int currentTime = 0;

        int totalWaitingTime = 0;

        int totalTurnaroundTime = 0;



        for (Process p : processes) {

            p.startTime = Math.max(currentTime, p.arrivalTime);

            p.endTime = p.startTime + p.burstTime;

            p.waitingTime = p.startTime - p.arrivalTime;

            p.turnaroundTime = p.endTime - p.arrivalTime;



            totalWaitingTime += p.waitingTime;

            totalTurnaroundTime += p.turnaroundTime;

            currentTime = p.endTime;



            db.insertResult(p.processId, "FCFS", p.arrivalTime, p.burstTime, p.startTime, p.endTime, p.waitingTime, p.turnaroundTime);

        }



        double avgWaitingTime = (double) totalWaitingTime / processes.size();

        double avgTurnaroundTime = (double) totalTurnaroundTime / processes.size();



        main.updateChart("FCFS", avgWaitingTime, avgTurnaroundTime);

    }

}



class SJF {

    public void schedule(List<Process> processes, Database db, Prince main) {

        processes.sort(Comparator.comparingInt(p -> p.burstTime));

        int currentTime = 0;

        int totalWaitingTime = 0;

        int totalTurnaroundTime = 0;



        for (Process p : processes) {

            p.startTime = Math.max(currentTime, p.arrivalTime);

            p.endTime = p.startTime + p.burstTime;

            p.waitingTime = p.startTime - p.arrivalTime;

            p.turnaroundTime = p.endTime - p.arrivalTime;



            totalWaitingTime += p.waitingTime;

            totalTurnaroundTime += p.turnaroundTime;

            currentTime = p.endTime;



            db.insertResult(p.processId, "SJF", p.arrivalTime, p.burstTime, p.startTime, p.endTime, p.waitingTime, p.turnaroundTime);

        }



        double avgWaitingTime = (double) totalWaitingTime / processes.size();

        double avgTurnaroundTime = (double) totalTurnaroundTime / processes.size();



        main.updateChart("SJF", avgWaitingTime, avgTurnaroundTime);

    }

}



class RoundRobin {

    public void schedule(List<Process> processes, int quantum, Database db, Prince main) {

        int currentTime = 0;

        int totalWaitingTime = 0;

        int totalTurnaroundTime = 0;



        Queue<Process> queue = new LinkedList<>(processes);



        while (!queue.isEmpty()) {

            Process p = queue.poll();

            if (p.remainingTime <= quantum) {

                currentTime += p.remainingTime;

                p.remainingTime = 0;

                p.endTime = currentTime;

                p.turnaroundTime = p.endTime - p.arrivalTime;

                p.waitingTime = p.turnaroundTime - p.burstTime;

            } else {

                currentTime += quantum;

                p.remainingTime -= quantum;

                queue.offer(p);

            }



            if (p.remainingTime == 0) {

                totalWaitingTime += p.waitingTime;

                totalTurnaroundTime += p.turnaroundTime;

                db.insertResult(p.processId, "Round Robin", p.arrivalTime, p.burstTime, p.startTime, p.endTime, p.waitingTime, p.turnaroundTime);

            }

        }



        double avgWaitingTime = (double) totalWaitingTime / processes.size();

        double avgTurnaroundTime = (double) totalTurnaroundTime / processes.size();



        main.updateChart("Round Robin", avgWaitingTime, avgTurnaroundTime);

    }

}

