package main.controllers;

import entity.Change;
import entity.Customer;
import entity.User;
import exception.DataSourceException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import main.HelloApplication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerUpdateController {
    User currentUser = User.getUserInstance();
    User helperUser = new User(currentUser.getId(), currentUser.getUsername(), currentUser.getPassword(), currentUser.getRole());
    @FXML
    private TextField firstName;
    @FXML
    private TextField lastName;

    @FXML
    private TableView<Customer> customerTableView;
    @FXML
    private TableColumn<Customer, String> firstNameColumn;
    @FXML
    private TableColumn<Customer, String> lastNameColumn;

    private List<Customer> allCustomers = new ArrayList<>();

    public void initialize() {
        firstNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().firstName()));
        lastNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().lastName()));

        allCustomers = HelloApplication.getDataSource().readAllCustomersFromDatabase();

        customerTableView.setItems(FXCollections.observableList(allCustomers));
        customerTableView.getSortOrder().addAll(lastNameColumn, firstNameColumn);
    }

    /**
     * Set of actions performed when user updates Customer in DB
     */

    public void update() {
        Customer selectedCustomer = customerTableView.getSelectionModel().getSelectedItem();


        ArrayList<String> messages = new ArrayList<>();

        if (firstName.getText().isBlank() && lastName.getText().isBlank()) {
            messages.add("Both fields are empty!");
        }

        if (selectedCustomer == null) {
            messages.add("You didn't select a customer!");
        }

        String firstNameDB, lastNameDB;

        if (firstName.getText().isBlank()) {
            firstNameDB = selectedCustomer.firstName();
        } else {
            firstNameDB = firstName.getText();
        }

        if (lastName.getText().isBlank()) {
            lastNameDB = selectedCustomer.lastName();
        } else {
            lastNameDB = lastName.getText();
        }

        Customer customerDB = new Customer(selectedCustomer.customerID(), firstNameDB, lastNameDB);

        if (messages.size() == 0) {
            var confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Would you like to update selected person?");

            Optional<ButtonType> result = confirmationAlert.showAndWait();
            if (result.get() == ButtonType.OK) {
                Customer changeHelperCustomer = new Customer(selectedCustomer.customerID(), selectedCustomer.firstName(), selectedCustomer.lastName());

                HelloApplication.getDataSource().updateCustomerInDatabase(customerDB);


                Change changeOne = new Change("firstName", selectedCustomer.firstName(), customerDB.firstName(), helperUser, LocalDateTime.now());
                Change changeTwo = new Change("lastName", selectedCustomer.lastName(), customerDB.lastName(), helperUser, LocalDateTime.now());

                List<Change> changeList = HelloApplication.getDataSource().loadAllChanges();
                changeList.add(changeOne);
                changeList.add(changeTwo);
                HelloApplication.getDataSource().writeChanges(changeList);


                firstName.clear();
                lastName.clear();

                initialize();
            }
        } else {
                String allMessages = String.join("\n", messages);
                var alert = new Alert(Alert.AlertType.ERROR, allMessages);
                alert.setTitle("Error while updating a customer!");
                alert.show();
            }
        }
    }
