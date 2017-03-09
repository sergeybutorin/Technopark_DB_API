package db.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import db.models.Forum;
import db.models.Post;
import db.models.Thread;
import db.models.User;
import db.services.ForumService;
import db.services.PostService;
import db.services.ThreadService;
import db.services.UserService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergey on 05.03.17.
 */
@SuppressWarnings("unchecked")
@RestController
class PostController {
    @Autowired
    private ThreadService threadService;

    @Autowired
    private UserService userService;

    @Autowired
    private ForumService forumService;

    @Autowired
    private PostService postService;

    @RequestMapping(path = "/api/thread/{thread}/create", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<Object> createPost(@PathVariable(value="thread") String threadSlugOrId, @RequestBody List<Post> body) {
        Thread t;
        try {
            t = threadService.getThreadById(Integer.parseInt(threadSlugOrId));
        } catch(NumberFormatException e) {
            t = threadService.getThreadBySlug(threadSlugOrId);
        }
        List<Post> posts = new ArrayList<>();
        for(Post postBody: body) {
            final String author = postBody.getAuthor();
            String created = postBody.getCreated();
            final String message = postBody.getMessage();
            final boolean isEdited = postBody.getIsEdited();
            if (created == null) {
                LocalDateTime a = LocalDateTime.now();
                created = a.toString() + "+03:00"; // получить актуальное время
            }
            final Post post = postService.create(new Post(author, created, message, isEdited, t.getId()));
            post.setForum(t.getForum());
            posts.add(post);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(PostListResponse(posts));
    }

    private static JSONObject PostDataResponse(Post post) {
        final JSONObject formDetailsJson = new JSONObject();
        formDetailsJson.put("author", post.getAuthor());
        formDetailsJson.put("created", post.getCreated());
        formDetailsJson.put("forum", post.getForum());
        formDetailsJson.put("id", post.getId());
        formDetailsJson.put("message", post.getMessage());
        formDetailsJson.put("isEdited", post.getIsEdited());
        formDetailsJson.put("thread", post.getThread());
        return formDetailsJson;
    }

    private String PostListResponse(List<Post> posts) {
        final JSONArray jsonArray = new JSONArray();

        for(Post p : posts) {
            if (p == null) {
                continue;
            }
            jsonArray.add(PostDataResponse(p));
        }
        return jsonArray.toString();
    }

}
