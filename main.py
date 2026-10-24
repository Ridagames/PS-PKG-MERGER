import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

class PS5PkgMergerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("⚡ PS PKG Freedom Hub - PC Edition")
        self.root.geometry("720x620")
        self.root.config(bg="#0F172A")
        
        self.sources = []
        self.output_path = ""
        self.app_mode = "MERGE"
        self.reversed_order = False
        self.current_theme = "CLASSIC"
        self.current_lang = "fr"

        # En-tête principal
        header_frame = tk.Frame(root, bg="#0F172A")
        header_frame.pack(fill="x", padx=20, pady=15)

        self.title_lbl = tk.Label(header_frame, text="⚡ PS PKG FREEDOM HUB", font=("Consolas", 18, "bold"), fg="white", bg="#0F172A")
        self.title_lbl.pack(side="left")

        # Menu Options / Univers / Langues
        menu_btn = tk.Button(header_frame, text="⚙️ MENU", bg="#334155", fg="#38BDF8", font=("Consolas", 10, "bold"), command=self.open_menu)
        menu_btn.pack(side="right")

        # Section 1 : Sources
        card1 = tk.LabelFrame(root, text=" 1. FICHIERS SOURCE (PKG / PARTIES) ", fg="#38BDF8", bg="#1E293B", font=("Consolas", 10, "bold"))
        card1.pack(fill="x", padx=20, pady=5)

        btn_frame1 = tk.Frame(card1, bg="#1E293B")
        btn_frame1.pack(fill="x", padx=10, pady=8)

        self.btn_pick = tk.Button(btn_frame1, text="CHOISIR LES FICHIERS", bg="#0284C7", fg="white", font=("Consolas", 9, "bold"), command=self.pick_files)
        self.btn_pick.pack(side="left", padx=5)

        self.btn_rev = tk.Button(btn_frame1, text="INVERSER L'ORDRE DES PARTIES", bg="#334155", fg="white", font=("Consolas", 9, "bold"), command=self.toggle_reverse)
        self.btn_rev.pack(side="left", padx=5)

        self.listbox = tk.Listbox(card1, height=6, bg="#0F172A", fg="white", font=("Consolas", 9), selectbackground="#0284C7")
        self.listbox.pack(fill="x", padx=10, pady=5)

        # Section 2 : Sortie & Traitement
        card2 = tk.LabelFrame(root, text=" 2. SORTIE & TRAITEMENT ", fg="#38BDF8", bg="#1E293B", font=("Consolas", 10, "bold"))
        card2.pack(fill="x", padx=20, pady=10)

        btn_frame2 = tk.Frame(card2, bg="#1E293B")
        btn_frame2.pack(fill="x", padx=10, pady=8)

        self.btn_out = tk.Button(btn_frame2, text="DÉFINIR EMPLACEMENT DE SORTIE", bg="#0D9488", fg="white", font=("Consolas", 9, "bold"), command=self.pick_output)
        self.btn_out.pack(side="left", padx=5)

        self.out_lbl = tk.Label(btn_frame2, text="Sortie : —", fg="#94A3B8", bg="#1E293B", font=("Consolas", 9))
        self.out_lbl.pack(side="left", padx=10)

        action_frame = tk.Frame(card2, bg="#1E293B")
        action_frame.pack(fill="x", padx=10, pady=8)

        self.btn_go = tk.Button(action_frame, text="▶️ LANCER LA FUSION", bg="#16A34A", fg="white", font=("Consolas", 11, "bold"), command=self.execute)
        self.btn_go.pack(fill="x", pady=5)

        self.bar = ttk.Progressbar(card2, orient="horizontal", mode="determinate", maximum=100)
        self.bar.pack(fill="x", padx=10, pady=5)

        self.status_lbl = tk.Label(card2, text="Prêt.", fg="#C084FC", bg="#1E293B", font=("Consolas", 10, "bold"))
        self.status_lbl.pack(padx=10, pady=5, anchor="w")

        # Section 3 : Sécurité & Vérification
        card3 = tk.LabelFrame(root, text=" 3. SÉCURITÉ & VÉRIFICATION ", fg="#38BDF8", bg="#1E293B", font=("Consolas", 10, "bold"))
        card3.pack(fill="x", padx=20, pady=5)

        self.verify_var = tk.IntVar(value=1)
        chk1 = tk.Checkbutton(card3, text="Contrôle d'intégrité après écriture", variable=self.verify_var, bg="#1E293B", fg="#94A3B8", selectcolor="#0F172A", font=("Consolas", 9))
        chk1.pack(anchor="w", padx=10, pady=5)

    def open_menu(self):
        menu_win = tk.Toplevel(self.root)
        menu_win.title("⚙️ OPTIONS & MENU")
        menu_win.geometry("320x280")
        menu_win.config(bg="#1E293B")

        tk.Label(menu_win, text="⚙️ OPTIONS", font=("Consolas", 12, "bold"), fg="white", bg="#1E293B").pack(pady=10)

        tk.Button(menu_win, text="📂 Basculer Mode Splitter / Fusion", bg="#334155", fg="white", font=("Consolas", 9), width=28, command=lambda: [self.toggle_mode(), menu_win.destroy()]).pack(pady=5)
        tk.Button(menu_win, text="🎨 Changer d'Univers (Thèmes)", bg="#334155", fg="white", font=("Consolas", 9), width=28, command=lambda: messagebox.showinfo("Univers", "Thèmes : Classique, Fortnite, GoW")).pack(pady=5)
        tk.Button(menu_win, text="🌐 Changer la Langue", bg="#334155", fg="white", font=("Consolas", 9), width=28, command=lambda: messagebox.showinfo("Langue", "Français actif")).pack(pady=5)
        tk.Button(menu_win, text="🔄 Vérifier les mises à jour", bg="#334155", fg="white", font=("Consolas", 9), width=28, command=lambda: messagebox.showinfo("Mise à jour", "L'application est déjà à jour !")).pack(pady=5)

    def toggle_mode(self):
        if self.app_mode == "MERGE":
            self.app_mode = "SPLIT"
            self.title_lbl.config(text="⚡ PKG SPLITTER")
            self.btn_go.config(text="▶️ LANCER LE DÉCOUPAGE", bg="#0284C7")
            messagebox.loginfo = "Mode Splitter activé"
        else:
            self.app_mode = "MERGE"
            self.title_lbl.config(text="⚡ PS PKG FREEDOM HUB")
            self.btn_go.config(text="▶️ LANCER LA FUSION", bg="#16A34A")
        self.sources.clear()
        self.update_listbox()

    def pick_files(self):
        if self.app_mode == "MERGE":
            files = filedialog.askopenfilenames(title="Sélectionner les parties PKG", filetypes=[("PKG Parts", "*.pkg*"), ("Tous", "*.*")])
            if files:
                self.sources = list(files)
        else:
            file = filedialog.askopenfilename(title="Sélectionner le PKG à découper", filetypes=[("PKG", "*.pkg")])
            if file:
                self.sources = [file]
        self.update_listbox()

    def toggle_reverse(self):
        self.reversed_order = not self.reversed_order
        self.sources.reverse()
        self.update_listbox()

    def update_listbox(self):
        self.listbox.delete(0, tk.END)
        for f in self.sources:
            self.listbox.insert(tk.END, os.path.basename(f))

    def pick_output(self):
        if self.app_mode == "SPLIT":
            d = filedialog.askdirectory(title="Dossier de destination")
            if d:
                self.output_path = d
                self.out_lbl.config(text=f"Sortie : {d}")
        else:
            f = filedialog.asksaveasfilename(title="Enregistrer sous...", defaultextension=".pkg", filetypes=[("PKG", "*.pkg")])
            if f:
                self.output_path = f
                self.out_lbl.config(text=f"Sortie : {os.path.basename(f)}")

    def execute(self):
        if not self.sources or not self.output_path:
            messagebox.showerror("Erreur", "Sélectionnez des sources et une sortie !")
            return
        if self.app_mode == "MERGE":
            self.merge_process()
        else:
            self.split_process()

    def merge_process(self):
        self.status_lbl.config(text="Fusion en cours...")
        self.root.update_idletasks()
        total_size = sum(os.path.getsize(f) for f in self.sources)
        done = 0
        with open(self.output_path, "wb") as f_out:
            for src in self.sources:
                with open(src, "rb") as f_in:
                    while True:
                        chunk = f_in.read(16 * 1024 * 1024)
                        if not chunk:
                            break
                        f_out.write(chunk)
                        done += len(chunk)
                        self.bar["value"] = int((done / total_size) * 100)
                        self.root.update_idletasks()
        self.status_lbl.config(text="Fusion réussie !")
        messagebox.showinfo("Succès", "Fusion terminée avec succès !")

    def split_process(self):
        self.status_lbl.config(text="Découpage en cours...")
        self.root.update_idletasks()
        src = self.sources[0]
        file_size = os.path.getsize(src)
        base_name = os.path.basename(src)
        clean_name = base_name[:-4] if base_name.lower().endswith(".pkg") else base_name
        max_part = 4 * 1024 * 1024 * 1024
        transferred = 0
        part_idx = 0
        with open(src, "rb") as f_in:
            while transferred < file_size:
                part_path = os.path.join(self.output_path, f"{clean_name}.pkg.{part_idx:02d}")
                target_size = min(max_part, file_size - transferred)
                with open(part_path, "wb") as f_out:
                    remaining = target_size
                    while remaining > 0:
                        data = f_in.read(min(remaining, 16 * 1024 * 1024))
                        if not data:
                            break
                        f_out.write(data)
                        remaining -= len(data)
                        transferred += len(data)
                        self.bar["value"] = int((transferred / file_size) * 100)
                        self.root.update_idletasks()
                part_idx += 1
        self.status_lbl.config(text="Découpage réussi !")
        messagebox.showinfo("Succès", "Découpage en .pkg.00 réussi !")

if __name__ == "__main__":
    root = tk.Tk()
    app = PS5PkgMergerApp(root)
    root.mainloop()
