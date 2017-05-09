package db.services;

import db.models.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by sergeybutorin on 09.03.17.
 */

@Service
@Transactional
public class VoteService {
    private final JdbcTemplate template;

    public VoteService(JdbcTemplate template) {
        this.template = template;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(VoteService.class.getName());

    public void clearTable() {
        final String clearTable = "TRUNCATE TABLE vote CASCADE";
        template.execute(clearTable);
        LOGGER.info("Table vote was dropped");
    }

    public void deleteTable() {
        final String dropTableVotes = "DROP TABLE IF EXISTS vote CASCADE";
        template.execute(dropTableVotes);
        LOGGER.info("Table vote was dropped");
    }

    public void createTable() {
        final String createTableVotes = "CREATE TABLE IF NOT EXISTS  vote (" +
                "id SERIAL NOT NULL PRIMARY KEY," +
                "user_id INT REFERENCES \"user\"(id) NOT NULL," +
                "voice SMALLINT," +
                "thread_id INT REFERENCES thread(id) NOT NULL," +
                "UNIQUE (user_id, thread_id))";
        template.execute(createTableVotes);
        LOGGER.info("Table vote was created!");
    }

    public int addVote(Vote vote) {
        final int count = template.queryForObject("SELECT COUNT(*) FROM vote WHERE user_id IN (SELECT id FROM \"user\" WHERE LOWER(nickname) = LOWER(?)) AND thread_id = ?",
                Mappers.countMapper, vote.getAuthor(), vote.getThreadId());
        if (count == 0) {
            final String query = "INSERT INTO vote (user_id, voice, thread_id) VALUES (" +
                    "(SELECT id FROM \"user\" WHERE LOWER(nickname) = LOWER(?)), ?, ?)";
            template.update(query, vote.getAuthor(), vote.getVoice(), vote.getThreadId());
            LOGGER.info("Vote added");
        } else {
            final String query = "UPDATE vote SET voice = ? " +
                    "WHERE user_id IN (SELECT id FROM \"user\" WHERE LOWER(nickname) = LOWER(?)) AND thread_id = ?";
            template.update(query, vote.getVoice(), vote.getAuthor(), vote.getThreadId());
            LOGGER.info("Vote updated");
        }
        return template.queryForObject("UPDATE thread SET votes = (SELECT SUM(voice) as votes FROM vote " +
                "WHERE (thread_id) = ?) WHERE id = ? RETURNING votes", Mappers.voteMapper, vote.getThreadId(), vote.getThreadId());
    }
}
