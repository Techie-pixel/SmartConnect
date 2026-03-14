package com.example.schoolmanagement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ChatNotificationService extends Service {

    private static final String TAG = "ChatNotifService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String FOREGROUND_CHANNEL_ID = "chat_service_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 9999;

    public static final String KEY_REPLY = "key_reply_text";

    // Set this from chat activities to suppress notifications for the active chat
    public static volatile String activeChat = null;
    public static volatile boolean activeFeedbackScreen = false;

    private String myUid = null;
    private String myRole = null; // "student", "teacher", or "principal"
    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;
    private final Map<String, ChildEventListener> messageListeners = new HashMap<>();

    // Feedback listener (admin only)
    private DatabaseReference feedbackRef;
    private ChildEventListener feedbackListener;

    // Ecommerce listener (non-admin only)
    private DatabaseReference ecomRef;
    private ChildEventListener ecomListener;
    // Assignments listeners
    private DatabaseReference assignmentsRef;
    private ChildEventListener assignmentsChildListener;
    private volatile boolean assignmentsInitialLoadDone = false;
    private final Map<String, ChildEventListener> submissionPerAssignmentListeners = new HashMap<>();
    private DatabaseReference submissionsRef;
    private ChildEventListener submissionsRootListener;
    private volatile boolean submissionsInitialLoadDone = false;
    private final java.util.Set<String> myAssignmentKeys = new java.util.HashSet<>();

    // Attendance listeners
    private DatabaseReference attendanceNotifRef;
    private ChildEventListener attendanceListener; // This listener is no longer used in the new structure
    private volatile boolean attendanceInitialLoadDone = false;
    private final Map<String, Map<String, ChildEventListener>> classSubjectAttendanceListeners = new HashMap<>();

    // Notices listeners
    private volatile boolean noticesStudentInitialLoadDone = false;
    private DatabaseReference noticesStudentRef;
    private ChildEventListener noticesStudentListener;

    private volatile boolean noticesTeacherInitialLoadDone = false;
    private DatabaseReference noticesTeacherRef;
    private ChildEventListener noticesTeacherListener;

    private volatile boolean noticesParentInitialLoadDone = false;
    private DatabaseReference noticesParentRef;
    private ChildEventListener noticesParentListener;

    // Timetable listeners
    private volatile boolean timetableStudentInitialLoadDone = false;
    private DatabaseReference timetableStudentRef;
    private ChildEventListener timetableStudentListener;

    private volatile boolean timetableTeacherInitialLoadDone = false;
    private final Map<String, Boolean> timetableTeacherInitialLoadMap = new HashMap<>();
    private final Map<String, ChildEventListener> timetableTeacherListeners = new HashMap<>();

    private volatile boolean timetableParentInitialLoadDone = false;
    private DatabaseReference timetableParentRef;
    private ChildEventListener timetableParentListener;

    private static final String[] STD_STREAM_KEYS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Show foreground notification (required for Android 8+)
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());

        // Determine logged-in user
        myUid = resolveMyUid();
        if (myUid == null || myUid.isEmpty()) {
            Log.w(TAG, "No logged-in user found, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start listening for chats
        listenForChats();

        // If admin, also listen for new feedback
        if ("admin".equals(myRole)) {
            listenForFeedback();
        } else {
            // Non-admin: listen for new e-commerce items
            listenForEcommerce();
        }

        // Assignments related notifications
        if ("student".equals(myRole)) {
            listenForAssignmentsForStudent();
            listenForSubmissionStatusForStudent();
            listenForNoticesForStudent();
            listenForAttendanceForStudent();
            listenForTimetableForStudent();
        } else if ("teacher".equals(myRole)) {
            listenForSubmissionsForTeacher();
            listenForNoticesForTeacher();
            listenForTimetableForTeacher();
        } else if ("parent".equals(myRole)) {
            listenForTimetableForParent();
            listenForNoticesForParent();
        }

        return START_STICKY;
    }

    private String resolveMyUid() {
        // 1. Check TeacherPrefs
        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String uid = tPrefs.getString("teacherId", null);
        if (uid != null && !uid.isEmpty()) {
            myRole = "teacher";
            return uid;
        }

        // 2. Check PrincipalPrefs
        SharedPreferences pPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        uid = pPrefs.getString("principalId", null);
        if (uid != null && !uid.isEmpty()) {
            myRole = "principal";
            return uid;
        }

        // 3. Check AdminPrefs
        SharedPreferences aPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        if (aPrefs.getBoolean("isAdminLoggedIn", false)) {
            myRole = "admin";
            return "admin";
        }

        // 4. Check ParentPrefs
        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        uid = parentPrefs.getString("parentKey", null);
        if (uid != null && !uid.isEmpty()) {
            myRole = "parent";
            return uid;
        }

        // 5. Check FirebaseAuth (students)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myRole = "student";
            return user.getUid();
        }

        return null;
    }

    // ── Feedback listener (admin only) ──
    private void listenForFeedback() {
        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback");
        feedbackListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Suppress if admin is viewing feedbacks screen
                if (activeFeedbackScreen)
                    return;

                String name = snapshot.child("Name").getValue(String.class);
                String feedback = snapshot.child("Feedback").getValue(String.class);

                String title = "New Feedback";
                if (name != null && !name.isEmpty()) {
                    title = "Feedback from " + name;
                }
                String body = (feedback != null && !feedback.isEmpty()) ? feedback : "New feedback received";

                showFeedbackNotification(title, body);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        feedbackRef.addChildEventListener(feedbackListener);
    }

    private void showFeedbackNotification(String title, String body) {
        Intent tapIntent = new Intent(this, AdminFeedbackActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notifId = "feedback_notif".hashCode() & 0xFFFFFFF;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, notifId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    // ── Ecommerce listener (non-admin) ──
    private volatile boolean ecomInitialLoadDone = false;

    private void listenForEcommerce() {
        ecomRef = FirebaseDatabase.getInstance().getReference("Ecommerce");

        // Step 1: Fetch existing items first so we know what's "old"
        ecomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Now we know existing items; mark initial load as done
                ecomInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ecomInitialLoadDone = true;
            }
        });

        // Step 2: Attach ChildEventListener — will fire for existing + new
        ecomListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Skip all events until initial load is complete
                if (!ecomInitialLoadDone)
                    return;

                String name = snapshot.child("name").getValue(String.class);
                Long priceLong = snapshot.child("price").getValue(Long.class);
                int price = priceLong != null ? priceLong.intValue() : 0;

                String title = "🛒 New Item Available!";
                String body = (name != null ? name : "New item") + " — ₹" + price;

                showEcommerceNotification(title, body);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        ecomRef.addChildEventListener(ecomListener);
    }

    private void listenForAssignmentsForStudent() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        if (std.isEmpty() || stream.isEmpty())
            return;
        String cat = std + "_" + stream;
        assignmentsRef = FirebaseDatabase.getInstance().getReference("Assignments").child(cat);

        assignmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignmentsInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                assignmentsInitialLoadDone = true;
            }
        });

        assignmentsChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!assignmentsInitialLoadDone)
                    return;
                String title = snapshot.child("title").getValue(String.class);
                showAssignmentNotification(title != null ? title : "Assignment", "New assignment uploaded");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        assignmentsRef.addChildEventListener(assignmentsChildListener);
    }

    private void showAssignmentNotification(String subject, String body) {
        int notifId = ("assign_" + subject).hashCode() & 0xFFFFFFF;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(subject)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    private void listenForSubmissionsForTeacher() {
        submissionsRef = FirebaseDatabase.getInstance().getReference("Submissions");
        DatabaseReference allAssignmentsRef = FirebaseDatabase.getInstance().getReference("Assignments");
        // Build my assignment key set
        allAssignmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myAssignmentKeys.clear();
                for (DataSnapshot cat : snapshot.getChildren()) {
                    for (DataSnapshot a : cat.getChildren()) {
                        String tId = a.child("teacherId").getValue(String.class);
                        if (tId != null && tId.equals(myUid)) {
                            String key = a.getKey();
                            if (key != null)
                                myAssignmentKeys.add(key);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        submissionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                submissionsInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                submissionsInitialLoadDone = true;
            }
        });

        submissionsRootListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String assignmentKey = snapshot.getKey();
                if (assignmentKey == null)
                    return;
                if (!submissionPerAssignmentListeners.containsKey(assignmentKey)) {
                    attachSubmissionListenerForAssignment(assignmentKey);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        submissionsRef.addChildEventListener(submissionsRootListener);
    }

    private void attachSubmissionListenerForAssignment(String assignmentKey) {
        ChildEventListener l = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!submissionsInitialLoadDone)
                    return;
                if (!myAssignmentKeys.contains(assignmentKey))
                    return;
                String studentName = snapshot.child("studentName").getValue(String.class);
                String title = "New Submission";
                String body = (studentName != null ? studentName : "A student") + " submitted an assignment";
                showAssignmentNotification(title, body);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        FirebaseDatabase.getInstance().getReference("Submissions").child(assignmentKey)
                .addChildEventListener(l);
        submissionPerAssignmentListeners.put(assignmentKey, l);
    }

    private void listenForSubmissionStatusForStudent() {
        submissionsRef = FirebaseDatabase.getInstance().getReference("Submissions");
        submissionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                submissionsInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                submissionsInitialLoadDone = true;
            }
        });
        submissionsRootListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Attach per-assignment listener to my submission node
                if (myUid == null || myUid.isEmpty())
                    return;
                DataSnapshot me = snapshot.child(myUid);
                if (me.exists()) {
                    attachMySubmissionListener(snapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (myUid == null || myUid.isEmpty())
                    return;
                DataSnapshot me = snapshot.child(myUid);
                if (me.exists()) {
                    attachMySubmissionListener(snapshot.getKey());
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        submissionsRef.addChildEventListener(submissionsRootListener);
    }

    private void attachMySubmissionListener(String assignmentKey) {
        if (assignmentKey == null)
            return;
        FirebaseDatabase.getInstance().getReference("Submissions").child(assignmentKey).child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    boolean firstLoaded = false;

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!firstLoaded) {
                            firstLoaded = true;
                            return;
                        }
                        String status = snapshot.child("status").getValue(String.class);
                        if ("checked".equals(status)) {
                            showAssignmentNotification("Assignment Checked", "Your assignment has been checked");
                        } else if ("rejected".equals(status)) {
                            String reason = snapshot.child("rejectionReason").getValue(String.class);
                            String body = "Assignment rejected"
                                    + (reason != null && !reason.isEmpty() ? ": " + reason : "");
                            showAssignmentNotification("Assignment Rejected", body);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void showEcommerceNotification(String title, String body) {
        int notifId = "ecom_notif".hashCode() & 0xFFFFFFF;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    // ── Notices listeners ──
    private void listenForNoticesForStudent() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        if (std.isEmpty() || stream.isEmpty())
            return;
        String cat = std + "_" + stream;
        noticesStudentRef = FirebaseDatabase.getInstance().getReference("Notices").child(cat);

        noticesStudentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticesStudentInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noticesStudentInitialLoadDone = true;
            }
        });

        noticesStudentListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!noticesStudentInitialLoadDone)
                    return;
                String info = snapshot.child("info").getValue(String.class);
                showNoticeNotification("New Notice Board Update", info != null ? info : "You have a new notice");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        noticesStudentRef.addChildEventListener(noticesStudentListener);
    }

    private void listenForNoticesForTeacher() {
        noticesTeacherRef = FirebaseDatabase.getInstance().getReference("Notices").child("TeacherNotices");

        noticesTeacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticesTeacherInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noticesTeacherInitialLoadDone = true;
            }
        });

        noticesTeacherListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!noticesTeacherInitialLoadDone)
                    return;
                String info = snapshot.child("info").getValue(String.class);
                showNoticeNotification("New Notice Board Update", info != null ? info : "You have a new notice");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        noticesTeacherRef.addChildEventListener(noticesTeacherListener);
    }

    private void listenForNoticesForParent() {
        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        String std = parentPrefs.getString("studentStandard", "");
        String stream = parentPrefs.getString("studentStream", "");
        if (std.isEmpty() || stream.isEmpty())
            return;
        String cat = std + "_" + stream;
        noticesParentRef = FirebaseDatabase.getInstance().getReference("Notices").child(cat);

        noticesParentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticesParentInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noticesParentInitialLoadDone = true;
            }
        });

        noticesParentListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!noticesParentInitialLoadDone)
                    return;
                if ("TeacherNotices".equals(snapshot.getKey()))
                    return;
                String info = snapshot.child("info").getValue(String.class);
                showNoticeNotification("New Notice Board Update",
                        info != null ? info : "You have a new notice for your child's class");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        noticesParentRef.addChildEventListener(noticesParentListener);
    }

    private void showNoticeNotification(String title, String body) {
        int notifId = ("notice_" + System.currentTimeMillis()).hashCode() & 0xFFFFFFF;

        Intent tapIntent;
        if ("student".equals(myRole)) {
            tapIntent = new Intent(this, StudentNoticesActivity.class);
        } else if ("teacher".equals(myRole)) {
            tapIntent = new Intent(this, TeacherNoticesActivity.class);
        } else {
            tapIntent = new Intent(this, ParentNoticesActivity.class);
        }
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, notifId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    private void listenForChats() {
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsListener = chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Find all chat keys that contain my UID
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String chatKey = chatSnap.getKey();
                    if (chatKey != null && chatKey.contains(myUid)) {
                        // Attach a child listener on this chat's messages if not already
                        if (!messageListeners.containsKey(chatKey)) {
                            attachMessageListener(chatKey);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: " + error.getMessage());
            }
        });
    }

    private void attachMessageListener(String chatKey) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatKey)
                .child("messages");

        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                studentchat.ChatMessage msg = snapshot.getValue(studentchat.ChatMessage.class);
                if (msg == null)
                    return;

                // Don't notify for my own messages
                if (msg.senderUid != null && msg.senderUid.equals(myUid))
                    return;

                // Don't notify if this chat is currently active (user is viewing it)
                if (chatKey.equals(activeChat))
                    return;

                // Determine the other user's UID
                String otherUid = getOtherUid(chatKey);

                // Build notification content
                String contentText;
                if (msg.imageBase64 != null && !msg.imageBase64.isEmpty()) {
                    contentText = "\uD83D\uDCF7 Photo";
                } else if (msg.text != null && !msg.text.isEmpty()) {
                    contentText = msg.text;
                } else {
                    contentText = "New message";
                }

                // Look up sender name and show notification
                lookupNameAndNotify(msg.senderUid, otherUid, chatKey, contentText);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        messagesRef.addChildEventListener(listener);
        messageListeners.put(chatKey, listener);
    }

    private String getOtherUid(String chatKey) {
        // Chat key format: uid1_uid2 (sorted)
        String[] parts = chatKey.split("_");
        if (parts.length == 2) {
            return parts[0].equals(myUid) ? parts[1] : parts[0];
        }
        return "";
    }

    private void lookupNameAndNotify(String senderUid, String otherUid, String chatKey, String contentText) {
        // Try Teachers node first
        FirebaseDatabase.getInstance().getReference("Teachers").child(senderUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            if (name == null)
                                name = "Teacher";
                            showChatNotification(name, contentText, chatKey, otherUid, senderUid);
                        } else {
                            // Try Students node
                            lookupStudentAndNotify(senderUid, otherUid, chatKey, contentText);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showChatNotification("User", contentText, chatKey, otherUid, senderUid);
                    }
                });
    }

    private void lookupStudentAndNotify(String senderUid, String otherUid, String chatKey, String contentText) {
        FirebaseDatabase.getInstance().getReference("Students").child(senderUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = "User";
                        if (snapshot.exists()) {
                            String n = snapshot.child("name").getValue(String.class);
                            if (n != null)
                                name = n;
                        } else {
                            // Try Principals node
                            lookupPrincipalAndNotify(senderUid, otherUid, chatKey, contentText);
                            return;
                        }
                        showChatNotification(name, contentText, chatKey, otherUid, senderUid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showChatNotification("User", contentText, chatKey, otherUid, senderUid);
                    }
                });
    }

    private void lookupPrincipalAndNotify(String senderUid, String otherUid, String chatKey, String contentText) {
        FirebaseDatabase.getInstance().getReference("Principals").child(senderUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = "User";
                        if (snapshot.exists()) {
                            String n = snapshot.child("name").getValue(String.class);
                            if (n != null)
                                name = n;
                        }
                        showChatNotification(name, contentText, chatKey, otherUid, senderUid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showChatNotification("User", contentText, chatKey, otherUid, senderUid);
                    }
                });
    }

    private void showChatNotification(String senderName, String contentText,
            String chatKey, String otherUid, String senderUid) {
        // Don't show if this chat is now active
        if (chatKey.equals(activeChat))
            return;

        // Tap intent — open the appropriate chat activity
        Intent tapIntent;
        if ("student".equals(myRole)) {
            tapIntent = new Intent(this, studentchat.class);
        } else {
            tapIntent = new Intent(this, teacherchat.class);
        }
        tapIntent.putExtra("otherUid", senderUid);
        tapIntent.putExtra("otherName", senderName);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notifId = chatKey.hashCode() & 0xFFFFFFF;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, notifId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Reply action
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_REPLY)
                .setLabel("Reply")
                .build();

        Intent replyIntent = new Intent(this, ChatReplyReceiver.class);
        replyIntent.putExtra("chatKey", chatKey);
        replyIntent.putExtra("myUid", myUid);
        replyIntent.putExtra("notifId", notifId);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                this, notifId + 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Reply",
                replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(senderName)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(replyAction);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    // ── Timetable Listeners ──
    private void listenForTimetableForStudent() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        if (std.isEmpty() || stream.isEmpty())
            return;
        String cat = std + "_" + stream;
        timetableStudentRef = FirebaseDatabase.getInstance().getReference("Timetable").child(cat);

        timetableStudentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                timetableStudentInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                timetableStudentInitialLoadDone = true;
            }
        });

        timetableStudentListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                if (!timetableStudentInitialLoadDone)
                    return;
                if ("TeacherTimetable".equals(snapshot.getKey()))
                    return;
                showTimetableNotification("New Timetable", "A new timetable has been uploaded for your class");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        timetableStudentRef.addChildEventListener(timetableStudentListener);
    }

    private void listenForTimetableForTeacher() {
        DatabaseReference timetableRef = FirebaseDatabase.getInstance().getReference("Timetable");
        for (String stdStream : STD_STREAM_KEYS) {
            DatabaseReference ref = timetableRef.child(stdStream).child("TeacherTimetable");
            timetableTeacherInitialLoadMap.put(stdStream, false);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot s) {
                    timetableTeacherInitialLoadMap.put(stdStream, true);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError e) {
                    timetableTeacherInitialLoadMap.put(stdStream, true);
                }
            });

            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                    Boolean loaded = timetableTeacherInitialLoadMap.get(stdStream);
                    if (loaded == null || !loaded)
                        return;
                    showTimetableNotification("New Timetable", "A new timetable has been shared with you");
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot s) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError e) {
                }
            };
            ref.addChildEventListener(listener);
            timetableTeacherListeners.put(stdStream, listener);
        }
    }

    private void listenForTimetableForParent() {
        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        String std = parentPrefs.getString("studentStandard", "");
        String stream = parentPrefs.getString("studentStream", "");
        if (std.isEmpty() || stream.isEmpty())
            return;
        String cat = std + "_" + stream;
        timetableParentRef = FirebaseDatabase.getInstance().getReference("Timetable").child(cat);

        timetableParentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                timetableParentInitialLoadDone = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                timetableParentInitialLoadDone = true;
            }
        });

        timetableParentListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                if (!timetableParentInitialLoadDone)
                    return;
                if ("TeacherTimetable".equals(snapshot.getKey()))
                    return;
                showTimetableNotification("New Timetable", "A new timetable has been uploaded for your child's class");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        timetableParentRef.addChildEventListener(timetableParentListener);
    }

    private void showTimetableNotification(String title, String body) {
        int notifId = ("timetable_" + System.currentTimeMillis()).hashCode() & 0xFFFFFFF;

        Intent tapIntent;
        if ("student".equals(myRole)) {
            tapIntent = new Intent(this, studenttimetable.class);
        } else if ("teacher".equals(myRole)) {
            tapIntent = new Intent(this, teachertimetable.class);
        } else {
            tapIntent = new Intent(this, ParentTimetableActivity.class);
        }
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, notifId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    // ── Attendance Listeners (Student) ──
    private void listenForAttendanceForStudent() {
        if (myUid == null)
            return;
        attendanceNotifRef = FirebaseDatabase.getInstance().getReference("AttendanceNotifications");

        // First, get the student's class and stream from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");

        if (std.isEmpty() || stream.isEmpty()) {
            Log.w(TAG, "Student Standard or Stream not found, cannot listen for attendance.");
            return;
        }

        String classPath = std + "_" + stream;
        setupClassSubjectAttendanceListener(classPath);
    }

    private void setupClassSubjectAttendanceListener(String classPath) {
        if (!classSubjectAttendanceListeners.containsKey(classPath)) {
            classSubjectAttendanceListeners.put(classPath, new HashMap<>());
        }

        // Listen to the class root to discover subjects
        attendanceNotifRef.child(classPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot subjectSnap, @Nullable String previousChildName) {
                String subjectName = subjectSnap.getKey();
                if (subjectName == null)
                    return;

                if (classSubjectAttendanceListeners.get(classPath).containsKey(subjectName))
                    return;

                ChildEventListener listener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dateSnap, @Nullable String previousChildName) {
                        checkAndNotifyAttendance(dateSnap, classPath, subjectName);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dateSnap, @Nullable String previousChildName) {
                        checkAndNotifyAttendance(dateSnap, classPath, subjectName);
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                };

                attendanceNotifRef.child(classPath).child(subjectName).addChildEventListener(listener);
                classSubjectAttendanceListeners.get(classPath).put(subjectName, listener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void checkAndNotifyAttendance(DataSnapshot dateSnap, String classPath, String subjectName) {
        if (myUid == null)
            return;
        String dateKey = dateSnap.getKey();
        if (dateKey == null)
            return;

        FirebaseDatabase.getInstance()
                .getReference("Attendance")
                .child(classPath)
                .child(subjectName)
                .child(dateKey)
                .child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String status = snapshot.child("status").getValue(String.class);
                            if (status != null) {
                                String title = "Attendance Marked";
                                String body = "You have been marked " + status + " in " + subjectName + " today.";
                                showAttendanceNotification(title, body);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read attendance for " + myUid + " on " + dateKey + ": "
                                + error.getMessage());
                    }
                });
    }

    private void showAttendanceNotification(String title, String body) {
        int notifId = (int) System.currentTimeMillis();
        Intent intent = new Intent(this, studentattendance.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.calendar) // Update icon if you have a specific attendance one
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null)
                return;

            // Chat messages channel
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_ID, "Chat Messages", NotificationManager.IMPORTANCE_HIGH);
            chatChannel.setDescription("Notifications for new chat messages");
            nm.createNotificationChannel(chatChannel);

            // Foreground service channel (low importance, silent)
            NotificationChannel serviceChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID, "Chat Service", NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setDescription("Keeps chat notifications active");
            nm.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("SmartConnect")
                .setContentText("Chat notifications active")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove all listeners
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
        for (Map.Entry<String, ChildEventListener> entry : messageListeners.entrySet()) {
            FirebaseDatabase.getInstance()
                    .getReference("Chats")
                    .child(entry.getKey())
                    .child("messages")
                    .removeEventListener(entry.getValue());
        }
        messageListeners.clear();
        // Remove feedback listener
        if (feedbackRef != null && feedbackListener != null) {
            feedbackRef.removeEventListener(feedbackListener);
        }
        // Remove ecommerce listener
        if (ecomRef != null && ecomListener != null) {
            ecomRef.removeEventListener(ecomListener);
        }
        // Remove notice listeners
        if (noticesStudentRef != null && noticesStudentListener != null) {
            noticesStudentRef.removeEventListener(noticesStudentListener);
        }
        if (noticesTeacherRef != null && noticesTeacherListener != null) {
            noticesTeacherRef.removeEventListener(noticesTeacherListener);
        }
        if (noticesParentRef != null && noticesParentListener != null) {
            noticesParentRef.removeEventListener(noticesParentListener);
        }
        // Remove attendance listener
        if (attendanceNotifRef != null && attendanceListener != null) {
            attendanceNotifRef.removeEventListener(attendanceListener);
        }
        // Remove timetable listeners
        if (timetableStudentRef != null && timetableStudentListener != null) {
            timetableStudentRef.removeEventListener(timetableStudentListener);
        }
        if (timetableParentRef != null && timetableParentListener != null) {
            timetableParentRef.removeEventListener(timetableParentListener);
        }
        DatabaseReference timetableRef = FirebaseDatabase.getInstance().getReference("Timetable");
        for (Map.Entry<String, ChildEventListener> entry : timetableTeacherListeners.entrySet()) {
            timetableRef.child(entry.getKey()).child("TeacherTimetable")
                    .removeEventListener(entry.getValue());
        }
        timetableTeacherListeners.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
