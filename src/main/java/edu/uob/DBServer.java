package edu.uob;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;

    private String db_name = "";

    Map<String, Boolean> preserved_table = new HashMap<>(){{
        put( "use", true);
        put("create", true);
        put("drop", true);
        put("alter", true);
        put("insert", true);
        put("select", true);
        put("update", true);
        put("join", true);
        put("and", true);
        put("or", true);
        put("delete", true);
        put("where", true);
        put("from", true);
        put("into", true);
        put("values", true);
        put("set", true);
        put("null", true);
        put("on", true);
        put("==", true);
        put(">", true);
        put("<", true);
        put(">=", true);
        put("<=", true);
        put("!=", true);
        put("like", true);
        put("true", true);
        put("false", true);
    }};

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
     * KEEP this signature otherwise we won't be able to mark your submission correctly.
     */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
     * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
     * able to mark your submission correctly.
     *
     * <p>This method handles all incoming DB commands and carries out the required actions.
     */
    public boolean checkName(String name){
        //[Letter] | [Digit] | [PlainText] [Letter] | [PlainText] [Digit]
        int n = name.length();
        for(int i=0;i<n;i++){
            char c = name.charAt(i);
            if(!Character.isLetterOrDigit(c) && c != '_'){
                return false;
            }
        }
        return true;
    }

    public void printTable(ArrayList<ArrayList<String>> table){
        int m = table.size();
        int n = table.get(0).size();
        for(int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (j != n - 1) {
                    System.out.print(table.get(i).get(j) + "|");
                }else{
                    System.out.print(table.get(i).get(j));
                }
            }
            System.out.println();
        }
    }

    public ArrayList<ArrayList<String>> readTable(String name){
        ArrayList<ArrayList<String>> table = new ArrayList<>();

        if(db_name.equals("")){
            System.out.println("[ERROR] Unknown database");
            return table;
        }

        String filePath = Paths.get("databases") + File.separator + db_name + File.separator + name +".tab";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {

                if(!line.trim().isEmpty()){
                    ArrayList<String> values = new ArrayList<>(Arrays.asList(line.split("\t", -1))); // 使用 -1 保持空字符串
                    table.add(values);
                }
            }
        } catch (IOException e) {
            System.out.println("Can not find table: " + e.getMessage());
            e.printStackTrace();
        }

        return table;
    }

    public void writeTable(String tableName, ArrayList<ArrayList<String>> table){
        String filePath = Paths.get("databases") + File.separator + db_name + File.separator + tableName + ".tab";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for(int i = 0; i < table.size(); i++){
                for(int j = 0; j < table.get(i).size(); j++){
                    writer.write(table.get(i).get(j));
                    if(j < table.get(i).size() - 1){
                        writer.write("\t");
                    }
                }
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            System.out.println("Cannot write table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ArrayList<String> spilt_token(String command){
        ArrayList<String> tokens = new ArrayList<>();
        String temp = "";
        int l = command.length();
        boolean inQuotes = false;

        for(int i = 0; i < l; i++){
            char c = command.charAt(i);

            if(c == '\'' && !inQuotes){
                inQuotes = true;
                temp += c;
            } else if(c == '\'' && inQuotes){
                inQuotes = false;
                temp += c;
                tokens.add(temp);
                temp = "";
            } else if(inQuotes){
                temp += c;
            } else if(Character.isLetterOrDigit(c) || c == '.' || c == '_'){
                temp += c;
            } else {
                if(!temp.equals("")){
                    tokens.add(temp);
                    temp = "";
                }
                if(c != ' ' && c != '\n' && c != '\t'){

                    if(i < l - 1 && c == '=' && command.charAt(i+1) == '='){
                        tokens.add("==");
                        i++;
                    } else if(i < l - 1 && c == '!' && command.charAt(i+1) == '='){
                        tokens.add("!=");
                        i++;
                    } else if(i < l - 1 && c == '>' && command.charAt(i+1) == '='){
                        tokens.add(">=");
                        i++;
                    } else if(i < l - 1 && c == '<' && command.charAt(i+1) == '='){
                        tokens.add("<=");
                        i++;
                    } else {
                        tokens.add(String.valueOf(c));
                    }
                }
            }
        }

        if(!temp.equals("")){
            tokens.add(temp);
        }

        return tokens;
    }

    public String useCommand(ArrayList<String> command){
        if(command.size() != 3){
            System.out.println("[ERROR] Error SQL syntax");
            return "[ERROR] Error SQL syntax";
        }else if(preserved_table.get(command.get(1).toLowerCase()) != null){
            System.out.println("[ERROR] Can not use preserved word as database name");
            return "[ERROR] Can not use preserved word as database name";
        }

        String db_path = Paths.get("databases") + File.separator + command.get(1);
        Path folderPath = Paths.get(db_path);
        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            db_name = command.get(1);
            System.out.println("[OK]");

        } else {
            System.out.println("[ERROR] Database does not exists");
            return "[ERROR] Database does not exists";
        }
        return "[OK]";
    }

    public String createCommand(ArrayList<String> command){
        if(command.size() < 4){
            return "[ERROR] Error SQL syntax";
        }

        int command_len = command.size();
        String createType = command.get(1).toLowerCase();

        if(createType.equals("database")){
            if(command.size() != 4){
                return "[ERROR] Error SQL syntax";
            } else if(preserved_table.get(command.get(2).toLowerCase()) != null){
                return "[ERROR] Can not use preserved word as database name";
            }else if(!checkName(command.get(2))){
                return "[ERROR] Database name can only compose by letters or digits";
            }

            String db_path = Paths.get("databases") + File.separator;
            Path folderPath = Paths.get(db_path + command.get(2));

            if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
                return "[ERROR] Database already exists";
            }else{
                try {
                    Files.createDirectories(folderPath);
                    return "[OK]";
                } catch (Exception e) {
                    return "[ERROR] Unable to create database";
                }
            }

        }else if(createType.equals("table")){
            if(!checkName(command.get(2))){
                return "[ERROR] Table name can only compose by letters or digits";
            }else if(preserved_table.get(command.get(2).toLowerCase()) != null){
                return "[ERROR] Can not use preserved word as table name";
            }else if(db_name.equals("")){
                return "[ERROR] Unknown database";
            }

            String db_path = Paths.get("databases") + File.separator;
            String filePath = db_path + db_name + File.separator + command.get(2) + ".tab";

            if (Files.exists(Paths.get(filePath))) {
                return "[ERROR] Table already exists";
            }else{
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

                    writer.write("id");

                    if(command.size() > 4){
                        if(!command.get(3).equals("(") || !command.get(command_len-2).equals(")")){
                            return "[ERROR] Error SQL syntax";
                        }

                        ArrayList<String> columns = new ArrayList<>();
                        for(int i = 4; i < command_len - 2; i++){
                            if(!command.get(i).equals(",")){
                                String columnName = command.get(i);

                                if(preserved_table.get(columnName.toLowerCase()) != null){
                                    return "[ERROR] Cannot use reserved keyword as column name";
                                }

                                if(columns.contains(columnName.toLowerCase())){
                                    return "[ERROR] Duplicate column name";
                                }

                                if(!checkName(columnName)){
                                    return "[ERROR] Invalid column name";
                                }

                                columns.add(columnName.toLowerCase());
                                writer.write("\t" + columnName);
                            }
                        }
                    }

                    writer.write("\n");
                    return "[OK]";
                } catch (IOException e) {
                    return "[ERROR] Unable to create table";
                }
            }
        }else{
            return "[ERROR] Error SQL syntax";
        }
    }

    public String dropCommand(ArrayList<String> command){

        if(command.size() != 4){
            return "[ERROR] Error SQL syntax, expected 4 tokens but got " + command.size();
        }

        String dropType = command.get(1).toLowerCase();
        String itemName = command.get(2);
        String path = Paths.get("databases") + File.separator;

        if(dropType.equals("database")){
            Path dbPath = Paths.get(path + itemName);
            if (Files.exists(dbPath) && Files.isDirectory(dbPath)) {
                try {
                    File folder = new File(path + itemName);
                    File[] files = folder.listFiles((dir, name) -> name.endsWith(".tab"));
                    if (files != null) {
                        for (File file : files) {
                            if(!file.delete()){
                                return "[ERROR] Unable to delete database files";
                            }
                        }
                    }
                    if(!folder.delete()){
                        return "[ERROR] Unable to delete database folder";
                    }

                    if(itemName.equals(db_name)){
                        db_name = "";
                    }
                    return "[OK]";
                } catch (Exception e) {
                    return "[ERROR] Unable to delete database: " + e.getMessage();
                }
            }else{
                return "[ERROR] Database does not exist";
            }

        }else if(dropType.equals("table")){
            if(db_name.equals("")){
                return "[ERROR] Unknown database";
            }

            Path tablePath = Paths.get(path + db_name + File.separator + itemName + ".tab");

            if(Files.exists(tablePath)){
                try{
                    File table = new File(path + db_name + File.separator + itemName + ".tab");
                    if(!table.delete()){
                        return "[ERROR] Unable to drop table";
                    }
                    return "[OK]";
                }catch (Exception e){
                    return "[ERROR] Unable to drop table: " + e.getMessage();
                }
            }else{
                return "[ERROR] Table does not exist in database";
            }
        }else{
            return "[ERROR] Error SQL syntax - invalid drop type";
        }
    }

    public String alterCommand(ArrayList<String> command){
        if(command.size() < 6){
            return "[ERROR] Error SQL syntax";
        }

        if(!command.get(1).toLowerCase().equals("table")){
            return "[ERROR] Error SQL syntax";
        }

        if(db_name.equals("")){
            return "[ERROR] Unknown database";
        }

        String tableName = command.get(2);
        String action = command.get(3).toLowerCase();

        ArrayList<ArrayList<String>> table = readTable(tableName);
        if(table.isEmpty()){
            return "[ERROR] Table does not exist";
        }

        if(action.equals("add")){
            if(command.size() != 6){
                return "[ERROR] Error SQL syntax";
            }

            String columnName = command.get(4);

            if(preserved_table.get(columnName.toLowerCase()) != null){
                return "[ERROR] Cannot use reserved keyword as column name";
            }

            for(String col : table.get(0)){
                if(col.toLowerCase().equals(columnName.toLowerCase())){
                    return "[ERROR] Column already exists";
                }
            }

            table.get(0).add(columnName);

            for(int i = 1; i < table.size(); i++){

                while(table.get(i).size() < table.get(0).size()){
                    table.get(i).add("");
                }
            }

            writeTable(tableName, table);
            return "[OK]";

        } else if(action.equals("drop")){
            if(command.size() != 6){
                return "[ERROR] Error SQL syntax";
            }

            String columnName = command.get(4);

            if(columnName.toLowerCase().equals("id")){
                return "[ERROR] Cannot drop id column";
            }

            int columnIndex = -1;
            for(int i = 0; i < table.get(0).size(); i++){
                if(table.get(0).get(i).toLowerCase().equals(columnName.toLowerCase())){
                    columnIndex = i;
                    break;
                }
            }

            if(columnIndex == -1){
                return "[ERROR] Column does not exist";
            }

            for(int i = 0; i < table.size(); i++){
                table.get(i).remove(columnIndex);
            }

            writeTable(tableName, table);
            return "[OK]";
        }

        return "[ERROR] Error SQL syntax";
    }

    public String insertCommand(ArrayList<String> command){
        //"INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        int command_len = command.size();

        if(db_name.equals("")){
            return "[ERROR] Unknown database";
        } else if(command_len < 7){
            return "[ERROR] Error SQL syntax";
        }

        if(!command.get(1).toLowerCase().equals("into")||!command.get(3).toLowerCase().equals("values")||!command.get(4).equals("(")||!command.get(command_len-2).equals(")")){
            return "[ERROR] Error SQL syntax";
        }

        ArrayList<ArrayList<String>> table = readTable(command.get(2));
        if(table.isEmpty()){
            return "[ERROR] Table does not exists";
        }

        int column_len = table.get(0).size();

        ArrayList<String> values = new ArrayList<>();
        for(int i = 5; i < command_len - 2; i++){
            if(!command.get(i).equals(",")){
                String value = command.get(i);

                if(value.startsWith("'") && value.endsWith("'")){
                    value = value.substring(1, value.length()-1);
                }
                values.add(value);
            }
        }

        if(values.size() != column_len - 1){
            return "[ERROR] Wrong number of values, expected " + (column_len - 1) + " but got " + values.size();
        }

        int nextId = 1;
        for(int i = 1; i < table.size(); i++){
            try{
                int currentId = Integer.parseInt(table.get(i).get(0));
                if(currentId >= nextId){
                    nextId = currentId + 1;
                }
            }catch(NumberFormatException e){

            }
        }

        ArrayList<String> newRow = new ArrayList<>();
        newRow.add(String.valueOf(nextId));
        newRow.addAll(values);

        table.add(newRow);

        writeTable(command.get(2), table);

        return "[OK]";
    }

    public boolean evaluateCondition(ArrayList<String> row, ArrayList<String> headers, ArrayList<String> condition) {
        return evaluateRecursive(row, headers, condition);
    }

    private boolean evaluateRecursive(ArrayList<String> row, ArrayList<String> headers, ArrayList<String> tokens) {
        if (tokens.size() < 1) return false;

        if (tokens.get(0).equals("(") && tokens.get(tokens.size() - 1).equals(")")) {
            return evaluateRecursive(row, headers, new ArrayList<>(tokens.subList(1, tokens.size() - 1)));
        }

        int depth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toLowerCase();
            if (token.equals("(")) depth++;
            else if (token.equals(")")) depth--;
            else if ((token.equals("and") || token.equals("or")) && depth == 0) {
                ArrayList<String> left = new ArrayList<>(tokens.subList(0, i));
                ArrayList<String> right = new ArrayList<>(tokens.subList(i + 1, tokens.size()));
                boolean leftResult = evaluateRecursive(row, headers, left);
                boolean rightResult = evaluateRecursive(row, headers, right);
                if (token.equals("and")) return leftResult && rightResult;
                else return leftResult || rightResult;
            }
        }

        if (tokens.size() == 3) {
            String attr = tokens.get(0);
            String op = tokens.get(1);
            String val = tokens.get(2);

            if (val.startsWith("'") && val.endsWith("'")) {
                val = val.substring(1, val.length() - 1);
            }

            int idx = -1;
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).equalsIgnoreCase(attr)) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) return false;

            String rowVal = row.get(idx);

            switch (op) {
                case "==": return rowVal.equals(val);
                case "!=": return !rowVal.equals(val);
                case ">":
                case ">=":
                case "<":
                case "<=":
                    try {
                        double a = Double.parseDouble(rowVal);
                        double b = Double.parseDouble(val);
                        return switch (op) {
                            case ">" -> a > b;
                            case ">=" -> a >= b;
                            case "<" -> a < b;
                            case "<=" -> a <= b;
                            default -> false;
                        };
                    } catch (Exception e) {
                        return rowVal.compareTo(val) > 0;
                    }
                case "like":
                    return rowVal.contains(val);
            }
        }

        if (tokens.size() == 1) {
            return tokens.get(0).equalsIgnoreCase("true");
        }

        return false;
    }

    public String selectCommand(ArrayList<String> command){
        //"SELECT " <WildAttribList> " FROM " [TableName] | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        int command_len = command.size();
        if(command_len<5){
            return "[ERROR] Error SQL syntax";
        }else if(db_name.equals("")){
            return "[ERROR] Unknown database";
        }

        int fromPos = -1;
        ArrayList<String> attrs = new ArrayList<>();

        for(int i=1;i<command_len-1;i++){
            if(command.get(i).toLowerCase().equals("from")){
                fromPos = i;
                break;
            }else if(!command.get(i).equals(",")){
                attrs.add(command.get(i));
            }
        }

        if(fromPos == -1 || fromPos+1 >= command_len){
            return "[ERROR] Error SQL syntax";
        }

        String tableName = command.get(fromPos+1);
        ArrayList<ArrayList<String>> table = readTable(tableName);

        if(table.isEmpty()){
            return "[ERROR] Table does not exist";
        }

        ArrayList<ArrayList<String>> resultTable = new ArrayList<>();
        ArrayList<String> headers = table.get(0);

        ArrayList<Integer> selectedColumns = new ArrayList<>();
        if(attrs.size() == 1 && attrs.get(0).equals("*")){
            for(int i = 0; i < headers.size(); i++){
                selectedColumns.add(i);
            }
        } else {
            for(String attr : attrs){
                for(int i = 0; i < headers.size(); i++){
                    if(headers.get(i).toLowerCase().equals(attr.toLowerCase())){
                        selectedColumns.add(i);
                        break;
                    }
                }
            }
        }

        ArrayList<String> resultHeader = new ArrayList<>();
        for(int col : selectedColumns){
            resultHeader.add(headers.get(col));
        }
        resultTable.add(resultHeader);

        if(fromPos + 2 < command_len - 1 && command.get(fromPos + 2).toLowerCase().equals("where")){
            ArrayList<String> condition = new ArrayList<>();
            for(int i = fromPos + 3; i < command_len - 1; i++){
                condition.add(command.get(i));
            }

            for(int i = 1; i < table.size(); i++){
                if(evaluateCondition(table.get(i), headers, condition)){
                    ArrayList<String> resultRow = new ArrayList<>();
                    for(int col : selectedColumns){
                        resultRow.add(table.get(i).get(col));
                    }
                    resultTable.add(resultRow);
                }
            }
        } else {
            for(int i = 1; i < table.size(); i++){
                ArrayList<String> resultRow = new ArrayList<>();
                for(int col : selectedColumns){
                    resultRow.add(table.get(i).get(col));
                }
                resultTable.add(resultRow);
            }
        }

        String result = "[OK]\n";
        for(ArrayList<String> row : resultTable){
            for(int i = 0; i < row.size(); i++){
                result += row.get(i);
                if(i < row.size() - 1) result += "\t";
            }
            result += "\n";
        }

        return result;
        //select with where
        //if(fromPos+2 < command_len-1 && command.get(fromPos+2).toLowerCase().equals("where")){
        //}
    }

    public String updateCommand(ArrayList<String> command){
        if(command.size() < 8){
            return "[ERROR] Error SQL syntax";
        }

        if(db_name.equals("")){
            return "[ERROR] Unknown database";
        }

        String tableName = command.get(1);

        if(!command.get(2).toLowerCase().equals("set")){
            return "[ERROR] Error SQL syntax";
        }

        String columnName = command.get(3);

        if(!command.get(4).equals("=")){
            return "[ERROR] Error SQL syntax";
        }

        String newValue = command.get(5);

        if(!command.get(6).toLowerCase().equals("where")){
            return "[ERROR] Error SQL syntax";
        }

        ArrayList<ArrayList<String>> table = readTable(tableName);
        if(table.isEmpty()){
            return "[ERROR] Table does not exist";
        }

        ArrayList<String> headers = table.get(0);

        int columnIndex = -1;
        for(int i = 0; i < headers.size(); i++){
            if(headers.get(i).toLowerCase().equals(columnName.toLowerCase())){
                columnIndex = i;
                break;
            }
        }

        if(columnIndex == -1){
            return "[ERROR] Column does not exist";
        }

        if(columnName.toLowerCase().equals("id")){
            return "[ERROR] Cannot update id column";
        }

        ArrayList<String> condition = new ArrayList<>();
        for(int i = 7; i < command.size() - 1; i++){
            condition.add(command.get(i));
        }

        for(int i = 1; i < table.size(); i++){
            if(evaluateCondition(table.get(i), headers, condition)){
                table.get(i).set(columnIndex, newValue);
            }
        }

        writeTable(tableName, table);
        return "[OK]";
    }

    public String deleteCommand(ArrayList<String> command){
        if(command.size() < 6){
            return "[ERROR] Error SQL syntax";
        }

        if(db_name.equals("")){
            return "[ERROR] Unknown database";
        }

        if(!command.get(1).toLowerCase().equals("from")){
            return "[ERROR] Error SQL syntax";
        }

        String tableName = command.get(2);

        if(!command.get(3).toLowerCase().equals("where")){
            return "[ERROR] Error SQL syntax";
        }

        ArrayList<ArrayList<String>> table = readTable(tableName);
        if(table.isEmpty()){
            return "[ERROR] Table does not exist";
        }

        ArrayList<String> headers = table.get(0);

        ArrayList<String> condition = new ArrayList<>();
        for(int i = 4; i < command.size() - 1; i++){
            condition.add(command.get(i));
        }

        for(int i = table.size() - 1; i >= 1; i--){
            if(evaluateCondition(table.get(i), headers, condition)){
                table.remove(i);
            }
        }

        writeTable(tableName, table);
        return "[OK]";
    }

    public String joinCommand(ArrayList<String> command){
        if(command.size() < 8){
            return "[ERROR] Error SQL syntax";
        }

        if(db_name.equals("")){
            return "[ERROR] Unknown database";
        }

        String table1Name = command.get(1);

        if(!command.get(2).toLowerCase().equals("and")){
            return "[ERROR] Error SQL syntax";
        }

        String table2Name = command.get(3);

        if(!command.get(4).toLowerCase().equals("on")){
            return "[ERROR] Error SQL syntax";
        }

        String attr1 = command.get(5);

        if(!command.get(6).toLowerCase().equals("and")){
            return "[ERROR] Error SQL syntax";
        }

        String attr2 = command.get(7);

        ArrayList<ArrayList<String>> table1 = readTable(table1Name);
        ArrayList<ArrayList<String>> table2 = readTable(table2Name);

        if(table1.isEmpty() || table2.isEmpty()){
            return "[ERROR] Table does not exist";
        }

        int attr1Index = -1, attr2Index = -1;
        for(int i = 0; i < table1.get(0).size(); i++){
            if(table1.get(0).get(i).toLowerCase().equals(attr1.toLowerCase())){
                attr1Index = i;
                break;
            }
        }

        for(int i = 0; i < table2.get(0).size(); i++){
            if(table2.get(0).get(i).toLowerCase().equals(attr2.toLowerCase())){
                attr2Index = i;
                break;
            }
        }

        if(attr1Index == -1 || attr2Index == -1){
            return "[ERROR] Attribute does not exist";
        }

        ArrayList<ArrayList<String>> result = new ArrayList<>();

        ArrayList<String> header = new ArrayList<>();
        header.add("id");
        for(int i = 1; i < table1.get(0).size(); i++){
            header.add(table1Name + "." + table1.get(0).get(i));
        }
        for(int i = 1; i < table2.get(0).size(); i++){
            header.add(table2Name + "." + table2.get(0).get(i));
        }
        result.add(header);

        int newId = 1;
        for(int i = 1; i < table1.size(); i++){
            for(int j = 1; j < table2.size(); j++){
                if(table1.get(i).get(attr1Index).equals(table2.get(j).get(attr2Index))){
                    ArrayList<String> joinedRow = new ArrayList<>();
                    joinedRow.add(String.valueOf(newId++));
                    for(int k = 1; k < table1.get(i).size(); k++){
                        joinedRow.add(table1.get(i).get(k));
                    }
                    for(int k = 1; k < table2.get(j).size(); k++){
                        joinedRow.add(table2.get(j).get(k));
                    }
                    result.add(joinedRow);
                }
            }
        }

        String resultStr = "[OK]\n";
        for(ArrayList<String> row : result){
            for(int i = 0; i < row.size(); i++){
                resultStr += row.get(i);
                if(i < row.size() - 1) resultStr += "\t";
            }
            resultStr += "\n";
        }

        return resultStr;
    }

    public String commandParser(ArrayList<String> command){
        int length = command.size();

        System.out.println("DEBUG: Command tokens count: " + length);
        for(int i = 0; i < length; i++){
            System.out.println("DEBUG: Token[" + i + "] = '" + command.get(i) + "'");
        }

        if(length == 0){
            return "[ERROR] Error SQL syntax";
        }

        if(!command.get(length-1).equals(";")){
            return "[ERROR] missing ';'";
        }

        String commandType = command.get(0).toLowerCase();
        String result = "";

        if(commandType.equals("use")){
            result = useCommand(command);
        }else if(commandType.equals("create")){
            result = createCommand(command);
        }else if(commandType.equals("drop")){
            result = dropCommand(command);
        }else if(commandType.equals("alter")){
            result = alterCommand(command);
        }else if(commandType.equals("insert")){
            result = insertCommand(command);
        }else if(commandType.equals("select")){
            result = selectCommand(command);
        }else if(commandType.equals("update")){
            result = updateCommand(command);
        }else if(commandType.equals("delete")){
            result = deleteCommand(command);
        }else if(commandType.equals("join")){
            result = joinCommand(command);
        }else{
            result = "[ERROR] Can not find the command type";
        }

        return result;
    }

    public String handleCommand(String command) {
        // TODO implement your server logic here
        if(command.length() == 0){
            System.out.println("[ERROR] Error SQL syntax");
            return "[ERROR] Error SQL syntax";
        }
        ArrayList<String> tokens = spilt_token(command);
        for(int i=0;i<tokens.size();i++){
            System.out.print(tokens.get(i)+"|");
        }
        System.out.print("\n");

        return commandParser(tokens);
    }

    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}