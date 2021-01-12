import java.util.List;

public class User {
    String nickname, password;
    USER_STATUS userStatus;
    //TODO descrittore della connessione

// ------ Constructors ------

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        userStatus = USER_STATUS.OFFLINE;
    }

// ------ Getters -------
    public String getNickname() {
        return nickname;
    }

    public USER_STATUS getUserStatus() {
        return userStatus;
    }

// ------ Setters -------

    public void login() {
        this.userStatus = USER_STATUS.ONLINE;
    }

    public void logout(){
        userStatus = USER_STATUS.OFFLINE;
    }

    // ------ Methods ------
    public boolean checkCredential(String nickname, String password){
        return this.nickname.equals(nickname) && this.password.equals(password);
    }
}
