import java.util.Scanner;

/**
 * Created by alexandr on 15.09.19.
 */
public class UserInterface {

    private DBRunner dbRunner;

    private Scanner scanner;

    UserInterface(DBRunner dbRunner){
        this.dbRunner = dbRunner;

        scanner = new Scanner(System.in);

        starter();
    }

    private void starter(){
        sayHello();
        checkExistingDataBase();
        waitCommand();
    }

    private void sayHello(){
        System.out.println("Здравствуйте\n");
    }


    private void checkExistingDataBase(){
        String s;
        System.out.println("Хотите подключится к существующей б.д.?(Y/N)\n");

        s = scanner.nextLine();


        while(s.charAt(0) != 'Y' && s.charAt(0) != 'y' &&
                s.charAt(0) != 'N' && s.charAt(0) != 'n'){
            System.out.println("Хотите подключится к существующей б.д.?(Y/N)\n");
            s = scanner.nextLine();
        }

        if(s.charAt(0) == 'Y' || s.charAt(0) == 'y'){
            getDataBase();
        } else {
            createDataBase();
        }
    }

    private void getDataBase(){
        String s;

        System.out.println("Введите имя базы данных:\n");
        s = scanner.nextLine();

        if(dbRunner.tryConnectToDB(s)){
            System.out.println("Успешно подключено к базе данных\n");
        } else {
            System.out.println("Не удалось найти данную базу\n");
            getDataBase();
        }
    }

    private void createDataBase(){
        String s;
        System.out.println("Введите имя для новой базы данных\n");
        s = scanner.nextLine();
        dbRunner.connectToDB(s);

        System.out.println("Введите путь до папки с блокчейном");
        s = scanner.nextLine();
        dbRunner.setPath(s);


        startParsing();
    }

    private void startParsing() {
        BitcoinParser bitcoinParser = dbRunner.initParsing();
        bitcoinParser.startParsing();
    }
/*
    private void checkExistingCollection(){
        String s;
        System.out.println("Хотите подключится к существующей коллекции?(Y/N)\n");


        s = scanner.nextLine();

        while(s.charAt(0) != 'Y' || s.charAt(0) != 'y' ||
                s.charAt(0) != 'N'|| s.charAt(0) != 'n'){
            System.out.println("Хотите подключится к существующей коллекции?(Y/N)\n");
            s = scanner.nextLine();
        }

        if(s.charAt(0) == 'Y' || s.charAt(0) == 'y'){
            chooseExistingCollection();
        } else {
            createCollection();
        }
    }

    private void chooseExistingCollection(){
        String s;
        System.out.println("Введите имя коллекции\n");
        s = scanner.nextLine();

        if(dbRunner.tryGetCollection(s)){
            System.out.println("Успешно подключено к коллекции\n");
        } else {
            System.out.println("Не удалось найти данную коллекцию\n");
            chooseExistingCollection();
        }
    }

    private void createCollection(){
        String s;
        System.out.println("Введите имя коллекции\n");
        s = scanner.nextLine();

        dbRunner.createCollection(s);
    }
    */

    private void waitCommand() {
        String s;

        System.out.println("Введите команду. help - для помощи");

        s = scanner.nextLine();

        if(s.equals("help")){
            showHelp();
        } else if(s.equals("quit")){
            quit();
        }

        waitCommand();
    }

    private void quit() {
        System.exit(0);
    }

    private void showHelp() {
        System.out.println("quit - для завершения приложения");
    }
}
