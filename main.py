import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

class PkgHubPCApp:
    def __init__(self, root):
        self.root = root
        self.root.title("⚡ PS PKG Freedom Hub - PC Edition")
        self.root.geometry("700x600")
        self.root.config(bg="#0F172A")
        
        self.sources = []
        self.output_path = ""
        self.app_mode = "MERGE"  # MERGE ou SPLIT
        self.reversed_order = False

        # --- En-tête ---
        header_frame = tk.Frame(root, bg="#0F172A")
        header_frame.pack(fill="x", padx=20, pady=15)

        title_lbl = tk.Label(header_frame, text="⚡ PS PKG FREEDOM HUB", font=("Consolas", 16, "bold"), fg="white", bg="#0F172A")
        title_lbl.pack(side="left")

        self.btn_mode = tk.Button(header_frame, text="📂 Mode : FUSION", bg="#334155", fg="#38BDF8", font=("Consolas", 9, "bold"), command=self.toggle_mode)
        self.btn_mode.pack(side="right")

        # --- Section 1 : Sources ---
        card1 = tk.LabelFrame(root, text=" 1. FICHIERS SOURCE ", fg="#38BDF8", bg="#1E293B", font=("Consolas", 10, "bold"))
        card1.pack(fill="x", padx=20, pady=5)

        btn_frame = tk.Frame(card1, bg="#1E293B")
        btn_frame.pack(fill="x", padx=10, pady=8)

        self.btn_pick_files = tk.Button(btn_frame, text="CHOISIR LES FICHIERS (.pkg / parties)", bg="#0284C7", fg="white", font=("Consolas", 9, "bold"), command=self.pick_files)
        self.btn_pick_files.pack(side="left", padx=5)

        self.btn_reverse = tk.Button(btn_frame, text="INVERSER L'ORDRE", bg="#334155", fg="white", font=("Consolas", 9, "bold"), command=self.toggle_reverse)
        self.btn_reverse.pack(side="left", padx=5)

        # Liste des fichiers sélectionnés
        self.listbox = tk.Listbox(card1, height=5, bg="#0F172A", fg="white", font=("Consolas", 9), selectbackground="#0284C7")
        self.listbox.pack(fill="x", padx=10, pady=5)

        # --- Section 2 : Sortie & Traitement ---
        card2 = tk.LabelFrame(root, text=" 2. SORTIE & TRAITEMENT ", fg="#38BDF8", bg="#1E293B", font=("Consolas", 10, "bold"))
        card2.pack(fill="x", padx=20, pady=10)

        out_frame = tk.Frame(card2, bg="#1E293B")
        out_frame.pack(fill="x", padx=10, pady=8)

        self.btn_out = tk.Button(out_frame, text="DÉFINIR EMPLACEMENT DE SORTIE", bg="#0D9488", fg="white", font=("Consolas", 9, "bold"), command=self.pick_output)
        self.btn_out.pack(side="left", padx=5)

        self.lbl_out = tk.Label(out_frame, text="Sortie : —", fg="#94A3B8", bg="#1E293B", font=("Consolas", 9))
        self.lbl_out.pack(side="left", padx=10)

        action_frame = tk.Frame(card2, bg="#1E293B")
        action_frame.pack(fill="x", padx=10, pady=8)

        self.btn_go = tk.Button(action_frame, text="▶️ LANCER LA FUSION", bg="#16A34A", fg="white", font=("Consolas", 11, "bold"), command=self.execute)
        self.btn_go.pack(fill="x", pady=5)

        self.bar = ttk.Progressbar(card2, orient="horizontal", mode="determinate", maximum=100)
        self.bar.pack(fill="x", padx=10, pady=5)

        self.status_lbl = tk.Label(card2, text="Prêt.", fg="#C084FC", bg="#1E293B", font=("Consolas", 10, "bold"))
        self.status_lbl.pack(padx=10, pady=5, anchor="w")

    def toggle_mode(self):
        if self.app_mode == "MERGE":
            self.app_mode = "SPLIT"
            self.btn_mode.config(text="📂 Mode : DÉCOUPAGE", fg="#4ADE80")
            self.btn_go.config(text="▶️ LANCER LE DÉCOUPAGE", bg="#0284C7")
        else:
            self.app_mode = "MERGE"
            self.btn_mode.config(text="📂 Mode : FUSION", fg="#38BDF8")
            self.btn_go.config(text="▶️ LANCER LA FUSION", bg="#16A34A")
        self.sources.clear()
        self.update_listbox()

    def pick_files(self):
        if self.app_mode == "MERGE":
            files = filedialog.askopenfilenames(title="Sélectionner les parties PKG", filetypes=[("PKG Parts", "*.pkg*"), ("Tous les fichiers", "*.*")])
            if files:
                self.sources = list(files)
        else:
            file = filedialog.askopenfilename(title="Sélectionner le fichier PKG à découper", filetypes=[("PKG File", "*.pkg"), ("Tous les fichiers", "*.*")])
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
            dir_path = filedialog.askdirectory(title="Choisir le dossier de destination des parties")
            if dir_path:
                self.output_path = dir_path
                self.lbl_out.config(text=f"Sortie : {dir_path}")
        else:
            file_path = filedialog.asksaveasfilename(title="Enregistrer le fichier fusionné sous...", defaultextension=".pkg", filetypes=[("PKG file", "*.pkg")])
            if file_path:
                self.output_path = file_path
                self.lbl_out.config(text=f"Sortie : {os.path.basename(file_path)}")

    def execute(self):
        if not self.sources:
            messagebox.showerror("Erreur", "Veuillez sélectionner des fichiers sources !")
            return
        if not self.output_path:
            messagebox.showerror("Erreur", "Veuillez définir l'emplacement de sortie !")
            return

        try:
            if self.app_mode == "MERGE":
                self.merge_process()
            else:
                self.split_process()
        except Exception as e:
            messagebox.showerror("Erreur critique", str(e))
            self.status_lbl.config(text="Échec du traitement.")

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
                        pct = int((done / total_size) * 100)
                        self.bar["value"] = pct
                        self.root.update_idletasks()

        self.status_lbl.config(text="Fusion réussie avec succès !")
        messagebox.showinfo("Succès", "La fusion est terminée !")

    def split_process(self):
        self.status_lbl.config(text="Découpage en cours...")
        self.root.update_idletasks()

        src = self.sources[0]
        file_size = os.path.getsize(src)
        base_name = os.path.basename(src)
        clean_name = base_name[:-4] if base_name.lower().endswith(".pkg") else base_name
        max_part = 4 * 1024 * 1024 * 1024  # 4 Go

        transferred = 0
        part_idx = 0

        with open(src, "rb") as f_in:
            while transferred < file_size:
                part_name = f"{clean_name}.pkg.{part_idx:02d}"
                part_path = os.path.join(self.output_path, part_name)
                target_size = min(max_part, file_size - transferred)

                with open(part_path, "wb") as f_out:
                    remaining = target_size
                    while remaining > 0:
                        chunk_size = min(remaining, 16 * 1024 * 1024)
                        data = f_in.read(chunk_size)
                        if not data:
                            break
                        f_out.write(data)
                        remaining -= len(data)
                        transferred += len(data)
                        pct = int((transferred / file_size) * 100)
                        self.bar["value"] = pct
                        self.root.update_idletasks()

                part_idx += 1

        self.status_lbl.config(text="Découpage réussi !")
        messagebox.showinfo("Succès", "Le découpage en parties .pkg.00... est terminé !")

if __name__ == "__main__":
    root = tk.Tk()
    app = PkgHubPCApp(root)
    root.mainloop()
