package com.op.back.post.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.post.dto.PostMediaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** Post 미디어 처리(업로드/정렬/루트 동기화) */
@Service
@RequiredArgsConstructor
public class PostMediaService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;

    public String detectMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return null;
        contentType = contentType.toLowerCase(Locale.ROOT);
        if (contentType.startsWith("image/")) return "IMAGE";
        if (contentType.startsWith("video/")) return "VIDEO";
        return null;
    }

    public void createMediaDocument(DocumentReference postRef, String uid, String postId,
                                    MultipartFile file, int order)
            throws IOException, ExecutionException, InterruptedException {

        String type = detectMediaType(file);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported media type");
        }

        String mediaId = UUID.randomUUID().toString();

        String mediaPath = "posts/" + uid + "/" + postId + "/media/" + mediaId;
        String mediaUrl = storageService.uploadFile(file, mediaPath);

        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("mediaUrl", mediaUrl);
        m.put("order", order);
        m.put("createdAt", Timestamp.now());

        postRef.collection("media").document(mediaId).set(m).get();
    }

    public List<DocumentSnapshot> getPostMediaDocs(DocumentReference postRef)
            throws ExecutionException, InterruptedException {
        return postRef.collection("media")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(d -> (DocumentSnapshot) d)
                .toList();
    }

    public List<PostMediaResponse> getPostMediaList(String postId) {
        try {
            DocumentReference postRef = firestore.collection("posts").document(postId);
            List<QueryDocumentSnapshot> docs = postRef.collection("media")
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .get()
                    .getDocuments();

            List<PostMediaResponse> res = new ArrayList<>();
            for (DocumentSnapshot d : docs) {
                Long ord = d.getLong("order");
                res.add(PostMediaResponse.builder()
                        .id(d.getId())
                        .type(d.getString("type"))
                        .mediaUrl(d.getString("mediaUrl"))
                        .order(ord != null ? ord.intValue() : 0)
                        .build());
            }
            return res;
        } catch (Exception e) {
            return List.of();
        }
    }

    public int getMediaCount(DocumentReference postRef)
            throws ExecutionException, InterruptedException {
        return postRef.collection("media")
                .get()
                .get()
                .size();
    }

    public int getMaxOrder(DocumentReference postRef)
            throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> docs = postRef.collection("media")
                .orderBy("order", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .get()
                .getDocuments();
        if (docs.isEmpty()) return -1;
        Long ord = docs.get(0).getLong("order");
        return ord != null ? ord.intValue() : -1;
    }

    public void normalizeOrders(DocumentReference postRef)
            throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> docs = postRef.collection("media")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .get()
                .getDocuments();

        int i = 0;
        for (DocumentSnapshot d : docs) {
            d.getReference().update("order", i++).get();
        }
    }

    public void syncFirstMediaToPostRoot(DocumentReference postRef)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> docs = postRef.collection("media")
                .orderBy("order", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .get()
                .getDocuments();

        Map<String, Object> updates = new HashMap<>();
        long count = postRef.collection("media").get().get().size();
        updates.put("mediaCount", count);

        if (docs.isEmpty()) {
            updates.put("mediaUrl", null);
            updates.put("mediaType", null);
        } else {
            DocumentSnapshot first = docs.get(0);
            updates.put("mediaUrl", first.getString("mediaUrl"));
            updates.put("mediaType", first.getString("type"));
        }

        postRef.update(updates).get();
    }

    public String getFirstMediaType(DocumentReference postRef)
            throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> docs = postRef.collection("media")
                .orderBy("order", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .get()
                .getDocuments();
        if (docs.isEmpty()) return null;
        return docs.get(0).getString("type");
    }

    public void cleanupAllMedia(DocumentReference postRef)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> mediaDocs = postRef
                .collection("media")
                .get()
                .get()
                .getDocuments();

        for (DocumentSnapshot m : mediaDocs) {
            String mediaUrl = m.getString("mediaUrl");
            storageService.deleteFileByUrl(mediaUrl);
        }
    }
}
