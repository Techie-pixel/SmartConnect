package com.example.schoolmanagement;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrincipalEcommerceActivity extends AppCompatActivity {

    private RecyclerView recyclerViewItems;
    private TextView tvCartItems, tvTotalAmount;
    private Button btnClearCart, btnConfirmOrder;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private Map<String, Integer> cartItems;
    private int totalAmount = 0;

    private String principalEmail = "";
    private String principalName = "Principal";

    private DatabaseReference ecomRef;

    private static final String TAG = "PrincipalEcommerce";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_ecommerce_new);

        // Get principal data from prefs
        SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        principalEmail = prefs.getString("principalEmail", "");
        principalName = prefs.getString("principalName", "Principal");

        // If email is empty, try to fetch from Firebase
        if (principalEmail == null || principalEmail.isEmpty()) {
            fetchPrincipalDataFromFirebase(prefs);
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();

        // Apply animations
        ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 100);
        
        android.widget.LinearLayout mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            UIAnimator.animateLinearLayoutItems(mainLayout, 300, 150);
        }

        ecomRef = FirebaseDatabase.getInstance().getReference("Ecommerce");
        loadItems();
    }

    private void fetchPrincipalDataFromFirebase(SharedPreferences prefs) {
        Log.d(TAG,
                "Fetching principal data from Firebase. principalId in prefs: " + prefs.getString("principalId", ""));

        String principalId = prefs.getString("principalId", "");

        // Try 1: Get from Firebase using saved principalId
        if (!principalId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Principals")
                    .child(principalId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "Principal snapshot exists: " + snapshot.exists());
                            if (snapshot.exists()) {
                                String email = snapshot.child("email").getValue(String.class);
                                String name = snapshot.child("name").getValue(String.class);
                                Log.d(TAG, "Retrieved email: " + email + ", name: " + name);
                                if (email != null) {
                                    principalEmail = email;
                                    prefs.edit().putString("principalEmail", principalEmail).apply();
                                }
                                if (name != null) {
                                    principalName = name;
                                    prefs.edit().putString("principalName", principalName).apply();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Database error: " + error.getMessage());
                            // Fallback to Firebase Auth current user
                            tryFallbackFromAuth(prefs);
                        }
                    });
        } else {
            // Try 2: Get from Firebase Auth current user
            tryFallbackFromAuth(prefs);
        }
    }

    private void tryFallbackFromAuth(SharedPreferences prefs) {
        Log.d(TAG, "Trying fallback from Firebase Auth");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();
            String displayName = currentUser.getDisplayName();
            Log.d(TAG, "Firebase Auth user: " + uid + ", email: " + email);

            if (email != null && !email.isEmpty()) {
                principalEmail = email;
                principalName = (displayName != null) ? displayName : "Principal";

                // Save to prefs for future use
                prefs.edit()
                        .putString("principalId", uid)
                        .putString("principalEmail", principalEmail)
                        .putString("principalName", principalName)
                        .apply();
                Log.d(TAG, "Successfully set principal data from Firebase Auth");
            } else {
                // Try 3: Query by email from Principals node
                if (currentUser.getEmail() != null) {
                    queryPrincipalByEmail(currentUser.getEmail());
                }
            }
        } else {
            Log.e(TAG, "No Firebase Auth user found - all fallbacks failed");
        }
    }

    private void queryPrincipalByEmail(String email) {
        Log.d(TAG, "Querying Principals node by email: " + email);

        FirebaseDatabase.getInstance().getReference("Principals")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Query result exists: " + snapshot.exists());
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String dbEmail = child.child("email").getValue(String.class);
                                String dbName = child.child("name").getValue(String.class);
                                if (dbEmail != null && dbEmail.equals(email)) {
                                    principalEmail = dbEmail;
                                    principalName = (dbName != null) ? dbName : "Principal";

                                    // Save principalId for future reference
                                    SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
                                    prefs.edit()
                                            .putString("principalId", child.getKey())
                                            .putString("principalEmail", principalEmail)
                                            .putString("principalName", principalName)
                                            .apply();
                                    Log.d(TAG, "Successfully retrieved principal data from query");
                                    return;
                                }
                            }
                            Log.e(TAG, "No principal found with email: " + email);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Query cancelled/error: " + error.getMessage());
                        // All fallbacks failed - user needs to re-login
                    }
                });
    }

    private void initViews() {
        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        tvCartItems = findViewById(R.id.tvCartItems);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnClearCart = findViewById(R.id.btnClearCart);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        cartItems = new HashMap<>();
    }

    private void setupRecyclerView() {
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter();
        recyclerViewItems.setAdapter(itemAdapter);
    }

    private void setupClickListeners() {
        btnClearCart.setOnClickListener(v -> clearCart());
        btnConfirmOrder.setOnClickListener(v -> confirmOrder());
    }

    private void loadItems() {
        ecomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    int price = 0;
                    Long priceLong = child.child("price").getValue(Long.class);
                    if (priceLong != null)
                        price = priceLong.intValue();
                    String image = child.child("imageBase64").getValue(String.class);
                    itemList.add(new Item(name, price, image));
                }
                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PrincipalEcommerceActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void updateCartUI() {
        StringBuilder cartText = new StringBuilder("Selected Items:\n");
        totalAmount = 0;

        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String name = entry.getKey();
            int qty = entry.getValue();
            int price = getItemPrice(name);
            int subtotal = qty * price;
            totalAmount += subtotal;
            cartText.append("✅ ").append(name)
                    .append("  x").append(qty)
                    .append("  = ₹").append(subtotal)
                    .append("\n");
        }

        if (cartItems.isEmpty()) {
            cartText = new StringBuilder("No items selected yet!\nTap checkboxes to add items ✨");
        }

        tvCartItems.setText(cartText.toString());
        tvTotalAmount.setText("Total: ₹" + totalAmount);
    }

    private int getItemPrice(String itemName) {
        for (Item item : itemList) {
            if (item.name != null && item.name.equals(itemName)) {
                return item.price;
            }
        }
        return 0;
    }

    private void clearCart() {
        cartItems.clear();
        updateCartUI();
        itemAdapter.notifyDataSetChanged();
        Toast.makeText(this, "🗑️ Cart cleared!", Toast.LENGTH_SHORT).show();
    }

    private void confirmOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Please select items first!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmOrder.setText("⏳ Processing...");
        btnConfirmOrder.setEnabled(false);

        // Final check - if still no email after all fallbacks, show helpful error
        if (principalEmail == null || principalEmail.isEmpty()) {
            showErrorAndReset("Principal email not found. Please logout and login again.");
            return;
        }

        final String orderDetails = formatOrderDetailsForEmail();
        final String email = principalEmail;
        final String name = principalName;
        final int total = totalAmount;

        new Thread(() -> {
            boolean emailSent = false;
            String errorMsg = "";

            if (email != null && !email.isEmpty()) {
                try {
                    String htmlBody = buildOrderEmailHtml(name, orderDetails, total);
                    GmailSender.sendHtmlMail(email,
                            "✅ Order Confirmed - Payment Successful",
                            htmlBody);
                    emailSent = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMsg = e.getMessage();
                }
            } else {
                errorMsg = "No email found for principal. Please update your profile.";
            }

            final boolean sent = emailSent;
            final String err = errorMsg;
            runOnUiThread(() -> {
                if (sent) {
                    Toast.makeText(this,
                            "🎉 Order Confirmed!\n📧 Email sent to " + email,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "🎉 Order Confirmed!\n⚠️ Email error: " + err,
                            Toast.LENGTH_LONG).show();
                }
                clearCart();
                btnConfirmOrder.setText("💳 Confirm and Pay Now");
                btnConfirmOrder.setEnabled(true);
            });
        }).start();
    }

    private void showErrorAndReset(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this,
                    "⚠️ Order Issue: " + message,
                    Toast.LENGTH_LONG).show();
            clearCart();
            btnConfirmOrder.setText("💳 Confirm and Pay Now");
            btnConfirmOrder.setEnabled(true);
        });
    }

    private String buildOrderEmailHtml(String userName, String orderDetails, int total) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;background:#f4f4f4;padding:20px}" +
                ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,.1);max-width:600px;margin:0 auto}"
                +
                ".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:20px;border-radius:8px;text-align:center}"
                +
                ".order-details{background:#f8f9fa;padding:20px;border-radius:8px;margin:20px 0;border-left:4px solid #4CAF50}"
                +
                ".item{padding:10px 0;border-bottom:1px solid #e0e0e0}" +
                ".total{background:#4CAF50;color:white;padding:15px;border-radius:8px;text-align:center;font-size:24px;font-weight:bold;margin-top:20px}"
                +
                "</style></head><body>" +
                "<div class='container'>" +
                "<div class='header'><div style='font-size:50px'>✅</div>" +
                "<h1 style='margin:0'>Order Successfully Placed!</h1>" +
                "<p style='margin:10px 0 0 0'>Payment Received</p></div>" +
                "<div style='padding:20px 0'>" +
                "<h2 style='color:#333'>Dear " + userName + ",</h2>" +
                "<p style='color:#666'>Your order has been confirmed. Your items will reach you as soon as possible!</p></div>"
                +
                "<div class='order-details'><h3 style='color:#333;margin-top:0'>📋 Order Details:</h3>" +
                orderDetails + "</div>" +
                "<div class='total'>💰 Total: ₹" + total + "</div>" +
                "<div style='margin-top:30px;padding:20px;background:#fff3cd;border-radius:8px;border-left:4px solid #ffc107'>"
                +
                "<p style='margin:0;color:#856404'><strong>📍 Note:</strong> We will deliver your items as early as possible.</p></div>"
                +
                "<div style='text-align:center;color:#888;margin-top:30px;font-size:12px'>" +
                "<p>Thank you for ordering! 🙏</p><p>— Smartconnect Team</p></div>" +
                "</div></body></html>";
    }

    private String formatOrderDetailsForEmail() {
        StringBuilder details = new StringBuilder("<div>");
        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String name = entry.getKey();
            int qty = entry.getValue();
            int price = getItemPrice(name);
            int subtotal = qty * price;
            details.append("<div class='item'>")
                    .append("<strong>").append(name).append("</strong><br>")
                    .append("Quantity: ").append(qty)
                    .append(" × ₹").append(price)
                    .append(" = <strong>₹").append(subtotal)
                    .append("</strong></div>");
        }
        details.append("</div>");
        return details.toString();
    }

    // ── Model ──
    private static class Item {
        String name;
        int price;
        String imageBase64;

        Item(String name, int price, String imageBase64) {
            this.name = name;
            this.price = price;
            this.imageBase64 = imageBase64;
        }
    }

    // ── Adapter ──
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            Item item = itemList.get(position);

            holder.itemName.setText(item.name != null ? item.name : "");
            holder.itemPrice.setText("₹" + item.price);
            holder.itemPrice.setTextColor(Color.parseColor("#4CAF50"));

            // Load product image from base64
            if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(item.imageBase64, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    holder.ivProduct.setImageBitmap(bmp);
                } catch (Exception e) {
                    holder.ivProduct.setImageResource(R.drawable.ic_default_profile);
                }
            } else {
                holder.ivProduct.setImageResource(R.drawable.ic_default_profile);
            }

            int qty = cartItems.getOrDefault(item.name, 0);
            holder.tvQty.setText(String.valueOf(qty));
            holder.checkBox.setChecked(qty > 0);

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!cartItems.containsKey(item.name)) {
                        cartItems.put(item.name, 1);
                        holder.tvQty.setText("1");
                    }
                    holder.cardView.setCardBackgroundColor(Color.parseColor("#402196F3"));
                } else {
                    cartItems.remove(item.name);
                    holder.tvQty.setText("0");
                    holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
                }
                updateCartUI();
            });

            holder.btnPlus.setOnClickListener(v -> {
                int current = cartItems.getOrDefault(item.name, 0);
                current++;
                cartItems.put(item.name, current);
                holder.tvQty.setText(String.valueOf(current));
                holder.checkBox.setChecked(true);
                holder.cardView.setCardBackgroundColor(Color.parseColor("#402196F3"));
                updateCartUI();
            });

            holder.btnMinus.setOnClickListener(v -> {
                int current = cartItems.getOrDefault(item.name, 0);
                if (current > 0) {
                    current--;
                    if (current == 0) {
                        cartItems.remove(item.name);
                        holder.checkBox.setChecked(false);
                        holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
                    } else {
                        cartItems.put(item.name, current);
                    }
                    holder.tvQty.setText(String.valueOf(current));
                    updateCartUI();
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemList != null ? itemList.size() : 0;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct;
            TextView itemName, itemPrice, tvQty;
            CheckBox checkBox;
            CardView cardView;
            Button btnPlus, btnMinus;

            ItemViewHolder(View itemView) {
                super(itemView);
                ivProduct = itemView.findViewById(R.id.ivProductImage);
                itemName = itemView.findViewById(R.id.itemName);
                itemPrice = itemView.findViewById(R.id.itemPrice);
                tvQty = itemView.findViewById(R.id.tvQty);
                checkBox = itemView.findViewById(R.id.itemCheckbox);
                cardView = itemView.findViewById(R.id.cardView);
                btnPlus = itemView.findViewById(R.id.btnPlus);
                btnMinus = itemView.findViewById(R.id.btnMinus);
            }
        }
    }
}
