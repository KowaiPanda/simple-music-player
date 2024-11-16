module application {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires javafx.graphics;
    requires javafx.base;
    requires org.xerial.sqlitejdbc;
    requires java.sql.rowset;
    requires jaudiotagger;

    opens application to javafx.fxml;
    exports application;
}
