package data;

import entity.*;
import exception.DataSourceException;
import javafx.scene.control.Alert;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class DatabaseAndFileDataSource implements DataSource, Closeable {
    private static final Path USER_FILE = Path.of("dat/user.txt");
    private final Connection connection;

    public DatabaseAndFileDataSource() throws DataSourceException, IOException {
        Properties properties = new Properties();
        properties.load(new FileReader("src/main/resources/db.properties"));

        String dbUrl = properties.getProperty("url");
        String dbUsername = properties.getProperty("username");
        String dbPassoword = properties.getProperty("password");

        try {
            connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassoword);
        } catch (SQLException e) {
            throw new DataSourceException(e);
        }
    }

    public UserRole userStringCompare(String role) {
        if (role.equals("ADMINISTRATION_ROLE")) {
            return UserRole.ADMINISTRATION_ROLE;
        } else if (role.equals("USER_ROLE")) {
            return UserRole.USER_ROLE;
        } else {
            return UserRole.UNKNOWN_ROLE;
        }
    }

    public Set<User> loadAllUsers() {
        Path userFile = USER_FILE;
        Set<User> userSet = new HashSet<>();

        try (Scanner scanner = new Scanner(userFile)) {
            while (scanner.hasNextLine()) {
                String[] fields = scanner.nextLine().split(";");

                long id = Long.parseLong(fields[0]);
                String username = fields[1];
                String password = fields[2];
                UserRole role = userRoleDetector(fields[3]);

                User user = new User(id, username, password, role);
                System.out.println(user);
                userSet.add(user);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + userFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return userSet;
    }

    public List<Customer> loadAllCustomersFromDatabase() {
        List<Customer> customerList = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM CUSTOMER");

            while (resultSet.next()) {
                Integer customerID = resultSet.getInt("CUSTOMER_ID");
                String firstName = resultSet.getString("FIRST_NAME");
                String lastName = resultSet.getString("LAST_NAME");

                Customer helpingCustomer = new Customer(customerID, firstName, lastName);
                customerList.add(helpingCustomer);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return customerList;
    }

    public void updateDataForCustomerInDatabase(Customer customer) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("UPDATE CUSTOMER SET FIRST_NAME = ?, LAST_NAME = ? WHERE CUSTOMER_ID = ? ");

            customerUpdateStatement.setString(1,customer.firstName());
            customerUpdateStatement.setString(2,customer.lastName());
            customerUpdateStatement.setInt(3,customer.customerID());

            customerUpdateStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createNewCustomerInDatabase(Customer customer) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("INSERT INTO CUSTOMER(FIRST_NAME, LAST_NAME) VALUES (?, ?)");

            customerUpdateStatement.setString(1,customer.firstName());
            customerUpdateStatement.setString(2,customer.lastName());

            customerUpdateStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteOldCustomerInDatabase(Customer customer) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("DELETE CUSTOMER WHERE CUSTOMER_ID = ? ");
            customerUpdateStatement.setInt(1,customer.customerID());

            try {
                customerUpdateStatement.executeUpdate();
            } catch (Exception e) {
                var alert = new Alert(Alert.AlertType.ERROR, "Customer is connected to a reservation! Delete the reservation first!");
                alert.setTitle("Error while trying to delete from database!");
                alert.show();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Comic> loadAllComicsFromDatabase() {
        List<Comic> comicList = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM COMIC");

            while (resultSet.next()) {
                Integer comicID = resultSet.getInt("COMIC_ID");
                String stringISBN = resultSet.getString("ISBN");
                String publisher = resultSet.getString("PUBLISHER");
                String comicName = resultSet.getString("COMIC_NAME");

                Publishers enumPublisher = publisherHelper(publisher);
                ISBN<String> ISBN = new ISBN<>(stringISBN);

                Comic helpingComic = new Comic(comicName, comicID, enumPublisher, ISBN);
                comicList.add(helpingComic);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return comicList;

    }

    private Publishers publisherHelper(String publisher) {
        for (Publishers p : Publishers.values()) {
            if (publisher.compareTo(p.toString()) == 0) {
                return p;
            }
        }
        return null;
    }

    private void createNewComicInDatabase(Comic comic) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("INSERT INTO COMIC(ISBN, PUBLISHER, COMIC_NAME) VALUES (?, ?, ?)");

            customerUpdateStatement.setString(1, comic.getIsbn().getISBNNumber());
            customerUpdateStatement.setString(2, comic.getPublisher().name());
            customerUpdateStatement.setString(3, comic.getBookName());

            customerUpdateStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteOldComicInDatabase(Comic selectedComic) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("DELETE COMIC WHERE COMIC_ID = ? ");
            customerUpdateStatement.setInt(1, selectedComic.getComicID());

            try {
                customerUpdateStatement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                var alert = new Alert(Alert.AlertType.ERROR, "Comic is connected to a reservation! Delete the reservation first!");
                alert.setTitle("Error while trying to delete from database!");
                alert.show();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateDataForComicInDatabase(Comic comic) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("UPDATE COMIC SET ISBN = ?, PUBLISHER = ?, COMIC_NAME = ? WHERE COMIC_ID = ? ");

            customerUpdateStatement.setString(1, comic.getIsbn().getISBNNumber());
            customerUpdateStatement.setString(2, comic.getPublisher().name());
            customerUpdateStatement.setString(3, comic.getBookName());
            customerUpdateStatement.setInt(4, comic.getComicID());

            customerUpdateStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Reservation> loadAllReservationsFromDatabase() {
        List<Reservation> reservationList = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM RESERVATION");

            while (resultSet.next()) {
                Integer reservationID = resultSet.getInt("RESERVATION_ID");
                Integer customerID = resultSet.getInt("CUSTOMER_ID");
                Integer comicID = resultSet.getInt("COMIC_ID");

                Customer customer = readCustomerWhereID(customerID).get();
                Comic comic = readComicID(comicID).get();

                Reservation helpingReservation = new Reservation(reservationID, customer, comic);
                reservationList.add(helpingReservation);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reservationList;
    }

    private Optional<Customer> readCustomerID(Integer ID) {
        try {
            PreparedStatement customerStatement = connection.prepareStatement("SELECT * FROM CUSTOMER WHERE CUSTOMER_ID = ?");
            customerStatement.setInt(1, ID);

            ResultSet resultSet = customerStatement.executeQuery();

            while (resultSet.next()) {
                Integer customerID = resultSet.getInt("CUSTOMER_ID");
                String firstName = resultSet.getString("FIRST_NAME");
                String lastName = resultSet.getString("LAST_NAME");

                Customer helpingCustomer = new Customer(customerID, firstName, lastName);
                return Optional.of(helpingCustomer);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return Optional.empty();
    }

    private Optional<Comic> readComicID(Integer ID) {
        try {
            PreparedStatement comicStatement = connection.prepareStatement("SELECT * FROM COMIC WHERE COMIC_ID = ?");
            comicStatement.setInt(1, ID);

            ResultSet resultSet = comicStatement.executeQuery();

            while (resultSet.next()) {
                Integer comicID = resultSet.getInt("COMIC_ID");
                String stringISBN = resultSet.getString("ISBN");
                String publisher = resultSet.getString("PUBLISHER");
                String comicName = resultSet.getString("COMIC_NAME");

                Publishers enumPublisher = publisherHelper(publisher);
                ISBN<String> ISBN = new ISBN<>(stringISBN);

                Comic helpingComic = new Comic(comicName, comicID, enumPublisher, ISBN);

                return Optional.of(helpingComic);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return Optional.empty();
    }

    private void createNewReservationInDatabase(Reservation reservation) {
        try {
            PreparedStatement customerUpdateStatement = connection.prepareStatement("INSERT INTO RESERVATION(CUSTOMER_ID, COMIC_ID) VALUES (?, ?)");

            customerUpdateStatement.setInt(1, reservation.getCustomer().customerID());
            customerUpdateStatement.setInt(2, reservation.getComic().getComicID());

            customerUpdateStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<User> readAllUsersFromFile() {
        return loadAllUsers();
    }

    @Override
    public UserRole userRoleDetector(String role) {
        return userStringCompare(role);
    }

    @Override
    public List<Customer> readAllCustomersFromDatabase() {
        return loadAllCustomersFromDatabase();
    }

    @Override
    public void createCustomerInDatabase(Customer customer) {
        createNewCustomerInDatabase(customer);
    }
    @Override
    public void updateCustomerInDatabase(Customer customer) {
        updateDataForCustomerInDatabase(customer);
    }

    @Override
    public void deleteCustomerInDatabase(Customer customer) {
        deleteOldCustomerInDatabase(customer);
    }

    @Override
    public List<Comic> readAllComicsFromDatabase() {
        return loadAllComicsFromDatabase();
    }

    @Override
    public void createComicInDatabase(Comic comic) {
        createNewComicInDatabase(comic);
    }

    @Override
    public void deleteComicInDatabase(Comic comic) {
        deleteOldComicInDatabase(comic);
    }

    @Override
    public void updateComicInDatabase(Comic comic) {
        updateDataForComicInDatabase(comic);
    }

    @Override
    public List<Reservation> readAllReservationsFromDatabase() {
        return loadAllReservationsFromDatabase();
    }

    @Override
    public Optional<Comic> readComicWhereID(Integer ID) {
        return readComicID(ID);
    }

    @Override
    public Optional<Customer> readCustomerWhereID(Integer ID) {
        return readCustomerID(ID);
    }

    @Override
    public void createReservationInDatabase(Reservation reservation) {
        createNewReservationInDatabase(reservation);
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException nothing) {

        }
    }
}



