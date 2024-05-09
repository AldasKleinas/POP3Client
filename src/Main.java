import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import javax.net.ssl.SSLSocketFactory;

public class Main {
    public static void main(String[] args) {
        System.out.println("Client started");
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket socket = factory.createSocket("pop.gmail.com", 995);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            if(!socket.isConnected()){
                System.out.println("Error. Connection to server failed.\n" +
                        "Shutting down...");
                System.exit(1);
            }

            if(!responseIsPositive(reader)){
                System.out.println("Error. Connection to server failed.\n" +
                        "Shutting down...");
                System.exit(2);
            }
            System.out.println("connected to server");

            String username;
            while(true){
                System.out.println("Enter your username.");
                username = input.readLine();
                writer.println("USER " + username);
                if(responseIsPositive(reader)){
                    System.out.println("Username valid.");
                    break;
                }
                System.out.println("Username not recognized. Try again.");
            }

            String password;
            while(true){
                System.out.println("Enter your password.");
                password = input.readLine();
                writer.println("PASS " + password);
                if(responseIsPositive(reader)){
                    System.out.println("Password valid.");
                    break;
                }
                System.out.println("Password incorrect. Try again.");
            }

            System.out.println("Welcome!");
            boolean end = false;
            while(!end){
                int optionId;
                ArrayList<Integer> mailSizes = getMailSizes(reader, writer);
                System.out.println("You currently have [" + mailSizes.size() + "] messages.\n" +
                        "Enter the Id of the action you wish to take:\n" +
                        "1) View a message\n" +
                        "2) Delete a message\n" +
                        "3) Restore deletions\n" +
                        "4) Ping Server\n" +
                        "5) Get message lengths\n" +
                        "6) Quit");
                optionId = getIndex(input, 6, "");

                switch(optionId){
                    case 1:
                        viewMessage(reader, writer, input, mailSizes.size());
                        break;
                    case 2:
                        deleteMessage(reader, writer, input, mailSizes.size());
                        break;
                    case 3:
                        restoreDeletions(reader, writer);
                        break;
                    case 4:
                        pingServer(reader, writer);
                        break;
                    case 5:
                        printMailSizes(reader, writer);
                        break;
                    case 6:
                        endTransaction(reader, writer);
                        end = true;
                        break;
                    default:
                        System.out.println("Unexpected error.");
                }
            }
            reader.close();
            writer.close();
            socket.close();
            System.out.println("System shutting down. Goodbye!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void endTransaction(BufferedReader reader, PrintWriter writer){
        writer.println("Quit");
        if(!responseIsPositive(reader)){
            System.out.println("Error. Command has failed.");
        }
        else{
            System.out.println("Shutdown initiated...");
        }
    }

    public static void printMailSizes(BufferedReader reader, PrintWriter writer){
        ArrayList<Integer> sizes;
        sizes = getMailSizes(reader, writer);

        if(sizes == null){
            System.out.println("You currently have no mail.");
        }
        else{
            int index = 1;
            for(int size : sizes){
                System.out.println("Size of message " + index + ": " + size);
                index++;
            }
        }
    }

    public static void pingServer(BufferedReader reader, PrintWriter writer){
        writer.println("Noop");
        if(responseIsPositive(reader)){
            System.out.println("Server is responsive.");
        }
        else{
            System.out.println("Server is not responding.");
        }
    }

    public static void deleteMessage(BufferedReader reader, PrintWriter writer, BufferedReader input, Integer count){
        int index = 0;
        index = getIndex(input, count, "Enter index of message you wish to delete.");

        writer.println("Dele " + index);
        if(!responseIsPositive(reader)){
            System.out.println("Error. Command failed.");
        }
        else{
            System.out.println("Message successfully deleted.");
        }
    }

    public static void restoreDeletions(BufferedReader reader, PrintWriter writer){
        writer.println("Rset");
        if(!responseIsPositive(reader)){
            System.out.println("Error. Command failed.");
        }
        else{
            System.out.println("Deleted messages successfully restored.");
        }
    }

    public static ArrayList<Integer> getMailSizes(BufferedReader reader, PrintWriter writer){
        writer.println("List");
        if(!responseIsPositive(reader)){
            return null;
        }

        ArrayList<String> buffer = new ArrayList<>();
        ArrayList<Integer> mailLengths = new ArrayList<>();
        try{
            String response;
            while((response = reader.readLine()) != null){
                if(response.equals(".")){
                    break;
                }

                buffer.add(response);
            }

            String[] parts;
            for(String str : buffer){
                parts = str.split(" ");
                mailLengths.add(Integer.parseInt(parts[1]));
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mailLengths;
    }

    public static void viewMessage(BufferedReader reader, PrintWriter writer, BufferedReader input, int count){
        int index = 0;
        index = getIndex(input, count, "Enter index of message you wish to view");

        writer.println("RETR " + index);
        if(!responseIsPositive(reader)){
            System.out.println("Error. Command failed.");
        }
        StringBuilder buffer = new StringBuilder();
        String response;
        while(true){
            try {
                if((response = reader.readLine()) != null){
                    if(response.equals(".")){
                        break;
                    }
                    buffer.append(response).append("\n");
                };
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println(buffer);
    }

    public static int getIndex(BufferedReader input, int max, String prompt){
        int index;
        while(true){
            try {
                System.out.println(prompt);
                index = Integer.parseInt(input.readLine());

            } catch (NumberFormatException | IOException e) {
                System.out.println("Invalid input. Enter an integer value.");
                continue;
            }
            if(index < 1 || index > max){
                System.out.println("Invalid input. Enter a value which is between 0 and " + max);
                continue;
            }
            break;
        }
        return index;
    }

    public static boolean responseIsPositive(BufferedReader reader){
        boolean result = false;
        String response;
        try {
            response = reader.readLine();
            if(response != null){
                String[] parts;
                parts = response.split(" ");
                if(parts[0].equalsIgnoreCase("+ok")){
                    result = true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}