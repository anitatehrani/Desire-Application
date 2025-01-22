package com.example.desire;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView desireRecyclerView;
    private DatabaseReference mDatabase;
    private String userId;
    private DesireAdapter desireAdapter;
    private List<Post> desireList;
    private List<Post> allPosts;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        desireRecyclerView = findViewById(R.id.desireScrollView);
        desireRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        desireList = new ArrayList<>();
        allPosts = new ArrayList<>();
        desireAdapter = new DesireAdapter(desireList);
        desireRecyclerView.setAdapter(desireAdapter);

        FirebaseUser currentUserAuth = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUserAuth != null) {
            userId = currentUserAuth.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize BottomNavigationBar
        View bottomNavigationView = findViewById(R.id.bottom_navigation);
        new BottomNavigationBar(this, bottomNavigationView, userId);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        loadUserData();

        // Set click listener for the Nearby button
        Button nearbyButton = findViewById(R.id.nearbyButton);
        nearbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NearbyActivity.class);
                startActivity(intent);
            }
        });


        // Initialize Add Desire button
        Button addDesireButton = findViewById(R.id.addDesireButton);
        addDesireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AddDesireActivity.class);
                startActivity(intent);

                // Apply fade-out animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

    }

    private void loadUserData() {
        DatabaseReference userRef = mDatabase.child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUser = dataSnapshot.getValue(User.class);
                if (currentUser != null) {
                    loadDesireItems();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDesireItems() {
        mDatabase.child("posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allPosts.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null && post.isVisibility() && !currentUser.rateddesire.contains(post.getPostId())) {
                        allPosts.add(post);
                    }
                }

                if (!allPosts.isEmpty()) {
                    desireList.clear();
                    desireList.add(allPosts.get(0));
                    desireAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class DesireAdapter extends RecyclerView.Adapter<DesireAdapter.DesireViewHolder> {
        private List<Post> posts;

        DesireAdapter(List<Post> posts) {
            this.posts = posts;
        }

        @NonNull
        @Override
        public DesireViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.desire_item, parent, false);
            return new DesireViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DesireViewHolder holder, int position) {
            Post post = posts.get(position);
            holder.bind(post);
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class DesireViewHolder extends RecyclerView.ViewHolder {
            ImageView myDesiresImage;
            TextView desireRating, desireComments, desireLocation, desireDate, desireDescription, username;
            ImageView[] stars;

            DesireViewHolder(View itemView) {
                super(itemView);
                myDesiresImage = itemView.findViewById(R.id.myDesiresImage);
                desireRating = itemView.findViewById(R.id.desireRating);
                desireComments = itemView.findViewById(R.id.desireComments);
                desireLocation = itemView.findViewById(R.id.desireLocation);
                desireDate = itemView.findViewById(R.id.desireDate);
                desireDescription = itemView.findViewById(R.id.desireDescription);
                username = itemView.findViewById(R.id.exploreusername);

                stars = new ImageView[]{
                        itemView.findViewById(R.id.star1),
                        itemView.findViewById(R.id.star2),
                        itemView.findViewById(R.id.star3),
                        itemView.findViewById(R.id.star4),
                        itemView.findViewById(R.id.star5)
                };
            }

            void bind(final Post post) {
                Glide.with(HomeActivity.this).load(post.getImageUrl()).into(myDesiresImage);
                desireRating.setText(String.valueOf(post.getRating()));
                desireComments.setText(String.valueOf(post.getCommentsCount()));
                desireLocation.setText(post.getLocation());
                desireDate.setText(post.getDate());
                desireDescription.setText(post.getDescription());
                username.setText(post.getUsername());
                setupStarRating(post);
            }

            private void setupStarRating(final Post post) {
                updateStarView(post.getRating());

                for (int i = 0; i < stars.length; i++) {
                    final int ratingIndex = i;
                    if (stars[i] != null) {
                        stars[i].setOnClickListener(v -> {
                            updateStarView(ratingIndex + 1);
                            updateRating(post, ratingIndex + 1);
                        });
                    }
                }
            }

            private void updateStarView(double rating) {
                for (int i = 0; i < stars.length; i++) {
                    if (stars[i] != null) {
                        stars[i].setImageResource(i < rating ? R.drawable.star_fill : R.drawable.star_empty);
                    }
                }
            }

            private void updateRating(Post post, int rating) {
                post.ratePost(rating);
                mDatabase.child("posts").child(post.getPostId()).setValue(post)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(HomeActivity.this, "Rated " + rating + " stars!", Toast.LENGTH_SHORT).show();

                                // Update user's rated desires list in Firebase
                                currentUser.rateddesire.add(post.getPostId());
                                mDatabase.child("users").child(userId).child("rateddesire").setValue(currentUser.rateddesire);

                                // Load next post if available
                                if (desireList.size() < allPosts.size()) {
                                    desireList.add(allPosts.get(desireList.size()));
                                    desireAdapter.notifyDataSetChanged();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "Failed to update rating", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}