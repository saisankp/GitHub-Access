package accessGitHubAPI;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDB {
	protected MongoClient client;
	private MongoDatabase db;
	private MongoCollection<Document> col;

	public MongoDB(String mongoUsername, String mongoPassword, String mongoClusterURL, String databaseName, String collectionName) {
		client = MongoClients.create("mongodb+srv://" + mongoUsername + ":" + mongoPassword + "@" + mongoClusterURL  + "/myFirstDatabase?retryWrites=true&w=majority");
		db = client.getDatabase(databaseName);
		col = db.getCollection(collectionName);
	}

	public void setDatabase(String databaseName) {
		db = client.getDatabase(databaseName);
	}

	public void setCollection(String collectionName) {
		col = db.getCollection(collectionName);
	}

	public MongoDatabase getDatabase() {
		return db;
	}

	public MongoCollection<Document> getCollection() {
		return col;
	}

	public void getAndStoreUserRepositoryInfo(GitHubClient client, String username) throws IOException {
		int id = 1;
		RepositoryService repoService = new RepositoryService(client);
		CommitService commitService = new CommitService(client);
		List<Repository> repoList = repoService.getRepositories(username);
		for (Repository repo : repoList) {
			if(id == repoList.size()) {
				ProgressBar.updateProgressBar(id, repoList.size(), "Stored repository data to MongoDB Atlas");
			}
			else {
				ProgressBar.updateProgressBar(id, repoList.size(), "Storing repository data to MongoDB Atlas");	
			}
			Document mongoDocument = new Document("_id", id);
			mongoDocument.append("RepositoryName", repo.getName());
			HashMap<String, List<String>> hm = new HashMap<>();
			List<String> list = new ArrayList<>();
			List<RepositoryCommit> repoCommits = null;
			try {
				repoCommits = commitService.getCommits(repo);
			} catch (Exception e) {}
			int numberOfCommits = 0;
			if(repoCommits != null) {
				for(RepositoryCommit commit : repoCommits) {				
					list = hm.getOrDefault(commit.getCommit().getAuthor().getName(), new ArrayList<>());
					list.add(commit.getCommit().getMessage().toString());
					hm.put(commit.getCommit().getAuthor().getName(), list);
					numberOfCommits++;
				}
			}
			mongoDocument.append("NumberOfCommits", numberOfCommits);
			mongoDocument.append("NumberOfForks", repo.getForks());
			mongoDocument.append("NumberOfWatchers", repo.getWatchers());
			mongoDocument.append("Language", repo.getLanguage());
			mongoDocument.append("Description", repo.getDescription());
			mongoDocument.append("Size", repo.getSize());
			mongoDocument.append("Creation", repo.getCreatedAt());
			mongoDocument.append("Updated", repo.getUpdatedAt());
			mongoDocument.append("URL", repo.getUrl());
			try {
				col.insertOne(mongoDocument);
			} catch (Exception e) {
				col.deleteOne(mongoDocument);
				Document failedMongoDocument = new Document("_id", id);
				mongoDocument.append("RepositoryName", repo.getName());
				mongoDocument.append("NumberOfForks", repo.getForks());
				mongoDocument.append("NumberOfWatchers", repo.getWatchers());
				mongoDocument.append("Language", repo.getLanguage());
				col.insertOne(failedMongoDocument);
				System.out.println("I had an oopsie.");
				e.printStackTrace();
			}
			id++;
		}
	}


	public void getAndStoreUserInfo(GitHubClient client, String username) throws IOException {
		UserService userService = new UserService(client);
		User user = userService.getUser(username);
		Document mongoDocument = new Document("_id", 1);
		ProgressBar.updateProgressBar(1, 12, "Storing user data to MongoDB Atlas");
		mongoDocument.append("UserName", username);
		mongoDocument.append("Name", user.getName());
		mongoDocument.append("Followers", user.getFollowers());
		mongoDocument.append("Following", user.getFollowing());
		mongoDocument.append("Location", user.getLocation());
		mongoDocument.append("Hirable", user.isHireable());
		mongoDocument.append("PublicRepositories", user.getPublicRepos());
		mongoDocument.append("LinkedBlogURL", user.getBlog());
		mongoDocument.append("AccountCreation", user.getCreatedAt());
		mongoDocument.append("GitHubAccountURL", user.getHtmlUrl());
		mongoDocument.append("GitHubAvatarURL", user.getAvatarUrl());
		ProgressBar.updateProgressBar(12, 12, "Stored user data to MongoDB Atlas");
		col.insertOne(mongoDocument);
	}

	public void clearCollection() {
		if(col.countDocuments() == 0) {
			return;
		}
		else {
			FindIterable<Document> findIterable = col.find();
			BasicDBObject document = new BasicDBObject();
			col.deleteMany(document);	
		}
	}

	public void insertDocument(Document mongoDocument) {
		col.insertOne(mongoDocument);
	}
}