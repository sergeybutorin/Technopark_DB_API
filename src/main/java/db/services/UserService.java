package db.services;

import db.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



/**
 * Created by sergey on 25.02.17.
 */

@Service
public final class UserService {
    private final JdbcTemplate template;
    private UserService(JdbcTemplate template) {
        this.template = template;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class.getName());

    public void clearTable() {
        final String dropTable = "DROP TABLE IF EXISTS \"user\" CASCADE";
        template.execute(dropTable);
        final String dropUniqueEmail = "DROP INDEX IF EXISTS unique_email";
        template.execute(dropUniqueEmail);
        final String dropUniqueNickname = "DROP INDEX IF EXISTS unique_nickname";
        template.execute(dropUniqueNickname);
        LOGGER.info("Table user dropped");
    }

    public void createTable() {
        final String createTable = "CREATE TABLE IF NOT EXISTS  \"user\" (" +
                "id SERIAL NOT NULL PRIMARY KEY," +
                "about TEXT," +
                "nickname VARCHAR(30) NOT NULL UNIQUE," +
                "fullname VARCHAR(100)," +
                "email VARCHAR(50) NOT NULL UNIQUE)";
        template.execute(createTable);
        final String createUniqueEmail = "CREATE UNIQUE INDEX unique_email ON \"user\" (LOWER(email))";
        template.execute(createUniqueEmail);
        final String createUniqueNickname = "CREATE UNIQUE INDEX unique_nickname ON \"user\" (LOWER(nickname))";
        template.execute(createUniqueNickname);
        LOGGER.info("Table user created!");
    }

    public User create(String about, String email, String fullname, String nickname) {
        final User user = new User(about, email, fullname, nickname);
        try {
            final String query = "INSERT INTO \"user\" (about, nickname, fullname, email) VALUES (?, ?, ?, ?)";
            template.update(query, about, nickname, fullname, email);
        }
        catch (DuplicateKeyException e) {
            LOGGER.info("Error creating user - user already exists!");
            return null;
        }
        LOGGER.info("User with nickname \"{}\" and email \"{}\" created", nickname, email);
        return user;
    }

    public User update(String about, String email, String fullname, String nickname) {
        final String query = "UPDATE \"user\" SET " +
                "about = COALESCE (?, about), " +
                "email = COALESCE (?, email), " +
                "fullname = COALESCE (?, fullname)" +
                "WHERE LOWER (nickname) = LOWER (?)";
        final int rows = template.update(query, about, email, fullname, nickname);
        if (rows == 0) {
            LOGGER.info("Error update user profile because user with such nickname does not exist!");
            return null;
        }
        return getUserByNickname(nickname);
    }

    public User getUserByNickname(String nickname) {
        try {
            return template.queryForObject("SELECT * FROM \"user\" WHERE LOWER (nickname) = ?", userMapper, nickname.toLowerCase());
        }
        catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getUserByEmail(String email) {
        try {
            return template.queryForObject("SELECT * FROM \"user\" WHERE LOWER (email) = ?", userMapper, email.toLowerCase());
        }
        catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getUserById(int id) {
        try {
            return template.queryForObject("SELECT * FROM \"user\" WHERE id = ?", userMapper, id);
        }
        catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private final RowMapper<User> userMapper = (rs, rowNum) -> {
        final int id = rs.getInt("id");
        final String about = rs.getString("about");
        final String nickname = rs.getString("nickname");
        final String fullname = rs.getString("fullname");
        final String email = rs.getString("email");
        return new User(id, about, email,fullname, nickname);
    };
}
