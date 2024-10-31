module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires javafx.graphics;
    requires javafx.base;
    requires org.xerial.sqlitejdbc;
    requires java.sql.rowset;
    requires jaudiotagger;

    opens com.example to javafx.fxml;
    exports com.example;
}
