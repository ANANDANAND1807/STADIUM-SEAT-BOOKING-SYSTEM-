import java.sql.*;
import java.util.Scanner;

public class StadiumBookingSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/stadium_booking"; 
    private static final String USER = "root";  
    private static final String PASS = "12345";  

    private static Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found. Include it in your library path.");
            return null;
        } catch (SQLException e) {
            System.out.println("Connection to the database failed: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Register User");
            System.out.println("2. View Events");
            System.out.println("3. View Available Seats");
            System.out.println("4. Book Ticket");
            System.out.println("5. View Booking History");
            System.out.println("6. Generate Ticket");
            System.out.println("7. Exit");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); 

            switch (choice) {
                case 1:
                    registerUser(scanner);
                    break;
                case 2:
                    viewEvents();
                    break;
                case 3:
                    viewAvailableSeats(scanner);
                    break;
                case 4:
                    bookTicket(scanner);
                    break;
                case 5:
                    viewBookingHistory(scanner);
                    break;
                case 6:
                    generateTicket(scanner);
                    break;
                case 7:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private static void registerUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, email, password) VALUES (?, ?, ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, password); 
                pstmt.executeUpdate();
                System.out.println("User registered successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    private static void viewEvents() {
        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM events")) {
                System.out.println("Available Events:");
                while (rs.next()) {
                    System.out.println("Event ID: " + rs.getInt("event_id") +
                            ", Name: " + rs.getString("event_name") +
                            ", Date: " + rs.getTimestamp("event_date") +
                            ", Available Seats: " + rs.getInt("available_seats") +
                            ", Ticket Price: $" + rs.getBigDecimal("ticket_price") +
                            ", Dynamic Price: $" + (rs.getBigDecimal("dynamic_pricing") != null ? rs.getBigDecimal("dynamic_pricing") : "N/A"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving events: " + e.getMessage());
        }
    }

    private static void viewAvailableSeats(Scanner scanner) {
        System.out.print("Enter event ID to view available seats: ");
        int eventId = scanner.nextInt();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            String query = "SELECT seat_number FROM seats WHERE event_id = ? AND status = 'available'";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, eventId);
                ResultSet rs = pstmt.executeQuery();

                System.out.println("Available Seats:");
                boolean found = false;
                while (rs.next()) {
                    System.out.println("Seat Number: " + rs.getString("seat_number"));
                    found = true;
                }
                if (!found) {
                    System.out.println("No available seats for this event.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving available seats: " + e.getMessage());
        }
    }

    private static void bookTicket(Scanner scanner) {
        System.out.print("Enter your user ID: ");
        int userId = scanner.nextInt();
        System.out.print("Enter event ID: ");
        int eventId = scanner.nextInt();
        System.out.print("Enter seat number: ");
        String seatNumber = scanner.next();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            
            String checkSeatQuery = "SELECT status FROM seats WHERE event_id = ? AND seat_number = ?";
            try (PreparedStatement checkSeatStmt = conn.prepareStatement(checkSeatQuery)) {
                checkSeatStmt.setInt(1, eventId);
                checkSeatStmt.setString(2, seatNumber);
                ResultSet rs = checkSeatStmt.executeQuery();

                if (rs.next() && "available".equals(rs.getString("status"))) {
                    
                    try (PreparedStatement bookStmt = conn.prepareStatement("INSERT INTO bookings (user_id, event_id, seat_number) VALUES (?, ?, ?)")) {
                        bookStmt.setInt(1, userId);
                        bookStmt.setInt(2, eventId);
                        bookStmt.setString(3, seatNumber);
                        bookStmt.executeUpdate();
                        System.out.println("Ticket booked successfully!");

                        
                        try (PreparedStatement updateSeatStmt = conn.prepareStatement("UPDATE seats SET status = 'booked' WHERE event_id = ? AND seat_number = ?")) {
                            updateSeatStmt.setInt(1, eventId);
                            updateSeatStmt.setString(2, seatNumber);
                            updateSeatStmt.executeUpdate();
                        }

                    
                        try (PreparedStatement updateEventsStmt = conn.prepareStatement("UPDATE events SET available_seats = available_seats - 1 WHERE event_id = ?")) {
                            updateEventsStmt.setInt(1, eventId);
                            updateEventsStmt.executeUpdate();
                        }
                    }
                } else {
                    System.out.println("Seat is not available for booking.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during booking: " + e.getMessage());
        }
    }

    private static void viewBookingHistory(Scanner scanner) {
        System.out.print("Enter your user ID: ");
        int userId = scanner.nextInt();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM bookings WHERE user_id = ?")) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                System.out.println("Booking History:");
                while (rs.next()) {
                    System.out.println("Booking ID: " + rs.getInt("booking_id") +
                            ", Event ID: " + rs.getInt("event_id") +
                            ", Seat Number: " + rs.getString("seat_number") +
                            ", Date: " + rs.getTimestamp("booking_date"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving booking history: " + e.getMessage());
        }
    }

    private static void generateTicket(Scanner scanner) {
        System.out.print("Enter your booking ID: ");
        int bookingId = scanner.nextInt();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.out.println("Failed to connect to the database.");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM bookings WHERE booking_id = ?")) {
                pstmt.setInt(1, bookingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Ticket Generated:");
                    System.out.println("Booking ID: " + rs.getInt("booking_id") +
                            ", User ID: " + rs.getInt("user_id") +
                            ", Event ID: " + rs.getInt("event_id") +
                            ", Seat Number: " + rs.getString("seat_number") +
                            ", Date: " + rs.getTimestamp("booking_date"));
                } else {
                    System.out.println("Booking not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error generating ticket: " + e.getMessage());
        }
    }

    private static void viewReports() {
        System.out.println("Viewing reports...");
    }
}