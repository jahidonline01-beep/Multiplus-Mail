package com.jahid.multiplusmail;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    RecyclerView recycler;
    EditText searchBar;
    ArrayList<MailAccount> all = new ArrayList<>();
    MailAdapter adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.appTitle);
        title.post(() -> {
            LinearGradient g = new LinearGradient(0, 0, title.getWidth(), 0,
                    new int[]{0xFFFF3755, 0xFFFFBE3C, 0xFFFF3DCE, 0xFF00DCFF},
                    null, Shader.TileMode.CLAMP);
            title.getPaint().setShader(g);
            title.invalidate();
        });

        recycler = findViewById(R.id.recyclerAccounts);
        searchBar = findViewById(R.id.searchBar);
        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(24);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));

        findViewById(R.id.btnAdd).setOnClickListener(v -> startActivity(new Intent(this, LoginPickerActivity.class)));
        findViewById(R.id.btnMenu).setOnClickListener(v -> premiumMenu());

        searchBar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { show(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        attachDragHelper();
    }

    @Override protected void onResume() {
        super.onResume();
        all = MailStorage.get(this);
        show(searchBar.getText().toString());
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int c, int r, int st) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        if (st != 0) g.setStroke(dp(1), st);
        return g;
    }

    TextView label(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(14), 0, dp(14), 0);
        return v;
    }

    void premiumMenu() {
        Dialog dialog = new Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackground(bg(0xFF070918, 26, 0xFF7C3AED));

        TextView header = label("Multiplus Mail", 24, Color.WHITE);
        header.setTypeface(null, Typeface.BOLD);
        box.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView clear = label("🧹   Clear Cache", 21, Color.WHITE);
        clear.setBackground(bg(0x1200DCFF, 18, 0));
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1, dp(58));
        clearLp.bottomMargin = dp(12);
        box.addView(clear, clearLp);

        LinearLayout admin = new LinearLayout(this);
        admin.setOrientation(LinearLayout.HORIZONTAL);
        admin.setGravity(Gravity.CENTER_VERTICAL);
        admin.setPadding(dp(12), dp(10), dp(12), dp(10));
        admin.setBackground(bg(0xFF062A3B, 22, 0xFF00DCFF));

        TextView plane = new TextView(this);
        plane.setText("✉");
        plane.setTextSize(28);
        plane.setGravity(Gravity.CENTER);
        plane.setTextColor(0xFF00DCFF);
        plane.setBackground(bg(0x1600DCFF, 18, 0x3300DCFF));
        admin.addView(plane, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);
        TextView t1 = new TextView(this);
        t1.setText("Telegram Admin Support");
        t1.setTextSize(18);
        t1.setTextColor(Color.WHITE);
        t1.setTypeface(null, Typeface.BOLD);
        TextView t2 = new TextView(this);
        t2.setText("https://t.me/JAHID_1");
        t2.setTextSize(14);
        t2.setTextColor(0xFF5EEBFF);
        info.addView(t1);
        info.addView(t2);
        TextView t3 = new TextView(this);
        t3.setText("Tap to open Telegram support");
        t3.setTextSize(11);
        t3.setTextColor(0xFFBFEFFF);
        info.addView(t3);
        admin.addView(info, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView open = new TextView(this);
        open.setText("Open ↗");
        open.setTextSize(16);
        open.setTextColor(0xFF00DCFF);
        open.setGravity(Gravity.CENTER);
        open.setTypeface(null, Typeface.BOLD);
        open.setBackground(bg(0x1A00DCFF, 16, 0xFF00DCFF));
        admin.addView(open, new LinearLayout.LayoutParams(dp(92), dp(52)));

        LinearLayout.LayoutParams adminLp = new LinearLayout.LayoutParams(-1, dp(88));
        adminLp.bottomMargin = dp(12);
        box.addView(admin, adminLp);

        TextView delete = label("🗑   Select & Delete", 21, Color.WHITE);
        delete.setBackground(bg(0x12FF3DCE, 18, 0));
        box.addView(delete, new LinearLayout.LayoutParams(-1, dp(58)));

        clear.setOnClickListener(v -> { clearCache(); dialog.dismiss(); });
        admin.setOnClickListener(v -> { openAdmin(); dialog.dismiss(); });
        open.setOnClickListener(v -> { openAdmin(); dialog.dismiss(); });
        delete.setOnClickListener(v -> { dialog.dismiss(); selectDeleteDialog(); });

        dialog.setContentView(box);
        dialog.show();
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setLayout((int) (getResources().getDisplayMetrics().widthPixels * .90f), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    Class<?> activityForSlot(int slot) {
        try {
            if (slot < 1) slot = 1;
            if (slot > 50) slot = 50;
            return Class.forName("com.jahid.multiplusmail.ContainerActivity" + slot);
        } catch (Exception e) {
            return ContainerActivity1.class;
        }
    }

    void openContainer(MailAccount a) {
        Intent i = new Intent(this, activityForSlot(a.slot));
        i.putExtra("id", a.id);
        i.putExtra("name", a.name);
        i.putExtra("provider", a.provider);
        i.putExtra("url", a.url);
        i.putExtra("slot", a.slot);
        startActivity(i);
    }

    void show(String q) {
        String query = q == null ? "" : q.toLowerCase();
        ArrayList<MailAccount> filtered = new ArrayList<>();
        for (MailAccount m : all) {
            if (m.name.toLowerCase().contains(query) || m.provider.toLowerCase().contains(query)) filtered.add(m);
        }
        adapter = new MailAdapter(filtered, new MailAdapter.Listener() {
            public void open(MailAccount a) { openContainer(a); }
            public void menu(MailAccount a) { menuDialog(a); }
        });
        recycler.setAdapter(adapter);
    }

    void attachDragHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                if (searchBar.getText().length() > 0) {
                    Toast.makeText(MainActivity.this, "Clear search before drag sorting", Toast.LENGTH_SHORT).show();
                    return false;
                }
                int a = from.getAdapterPosition();
                int b = to.getAdapterPosition();
                if (a < 0 || b < 0) return false;
                Collections.swap(all, a, b);
                MailStorage.save(MainActivity.this, all);
                show("");
                return true;
            }
            public void onSwiped(RecyclerView.ViewHolder v, int d) {}
            public boolean isLongPressDragEnabled() { return true; }
        }).attachToRecyclerView(recycler);
    }

    void clearCache() {
        try {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    void openAdmin() {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JAHID_1"))); }
        catch (Exception ignored) {}
    }

    void selectDeleteDialog() {
        ArrayList<MailAccount> list = MailStorage.get(this);
        if (list.size() == 0) {
            Toast.makeText(this, "No containers", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[list.size()];
        boolean[] checked = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) names[i] = list.get(i).name + " · Slot " + list.get(i).slot;

        new AlertDialog.Builder(this)
                .setTitle("Select & Delete")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Delete", (d, w) -> {
                    for (int i = 0; i < checked.length; i++) if (checked[i]) MailStorage.del(this, list.get(i).id);
                    all = MailStorage.get(this);
                    show(searchBar.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void menuDialog(MailAccount a) {
        new AlertDialog.Builder(this)
                .setTitle(a.name)
                .setItems(new String[]{"Rename", "Open", "Delete"}, (d, w) -> {
                    if (w == 0) rename(a);
                    if (w == 1) openContainer(a);
                    if (w == 2) {
                        MailStorage.del(this, a.id);
                        all = MailStorage.get(this);
                        show(searchBar.getText().toString());
                    }
                }).show();
    }

    void rename(MailAccount a) {
        EditText input = new EditText(this);
        input.setText(a.name);
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Rename Container")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) {
                        MailStorage.rename(this, a.id, n);
                        all = MailStorage.get(this);
                        show(searchBar.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
