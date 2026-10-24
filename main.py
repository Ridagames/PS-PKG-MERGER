import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

class PkgHubApp:
    def __init__(self, root):
        self.root = root
        self.root.title("⚡ PS PKG Freedom Hub - PC Edition")
        self.root.geometry("650x550")
        self.root.config(bg="#0F172A")
        
        self.source_path = ""
        self.output_path = ""
        
        # Titre
        title_label = tk.Label(root, text="⚡ PS PKG FREEDOM HUB (PC)", font=("Arial", 16, "bold"), fg="#38BDF8", bg="#0F172A")
        title_label.pack(pady=15)
        
        # Section Source
        frame_src = tk.LabelFrame(root, text=" 1. Fichier Source (.PKG ou Dossier) ", fg="white", bg="#1E293B", font=("Arial", 10, "bold"))
        frame_src.pack(fill="x", padx=20, pady=10)
        
        self.lbl_src = tk.Label(frame_src, text="Aucun fichier sélectionné", fg="#94A3B8", bg="#1E293B", wraplength=550, justify="left")
        self.lbl_src.pack(padx=10, pady=5, anchor="w")
        
        btn_pick_file = tk.Button(frame_src, text="📁 Choisir un .PKG (Découpage)", bg="#0284C7", fg="white", font=("Arial", 9, "bold"), command=self.pick_source_file)
        btn_pick_file.pack(padx=10, pady=5, anchor="w")

        btn_pick_dir = tk.Button(frame_src, text="📂 Choisir un dossier (Fusion)", bg="#0369A1", fg="white", font=("Arial", 9, "bold"), command=self.pick_source_dir)
        btn_pick_dir.pack(padx=10, pady=5, anchor="w")

        # Section Sortie
        frame_out = tk.LabelFrame(root, text=" 2. Emplacement de Sortie ", fg="white", bg="#1E293B", font=("Arial", 10, "bold"))
        frame_out.pack(fill="x", padx=20, pady=10)
        
        self.lbl_out = tk.Label(frame_out, text="Aucun emplacement défini", fg="#94A3B8", bg="#1E293B", wraplength=550, justify="left")
        self.lbl_out.pack(padx=10, pady=5, anchor="w")
        
        btn_pick_out = tk.Button(frame_out, text="🎯 Définir le dossier de sortie", bg="#0D9488", fg="white", font=("Arial", 9, "bold"), command=self.pick_output_dir)
        btn_pick_out.pack(padx=10, pady=5, anchor="w")

        # Section Action / Progression
        frame_action = tk.Frame(root, bg="#0F172A")
        frame_action.pack(fill="x", padx=20, pady=15)

        self.btn_go = tk.Button(frame_action, text="▶️ LANCER LE TRAITEMENT", bg="#16A34A", fg="white", font=("Arial", 11, "bold"), command=self.execute_action)
        self.btn_go.pack(fill="x", pady=5)

        self.progress = ttk.Progressbar(frame_action, orient="horizontal", length=500, mode="determinate")
        self.progress.pack(fill="x", pady=10)

        self.lbl_status = tk.Label(root, text="Prêt.", fg="#C084FC", bg="#0F172A", font=("Arial", 10, "bold"))
        self.lbl_status.pack(pady=5)

    def pick_source_file(self):
        filename = filedialog.askopenfilename(title="Sélectionner un fichier PKG", filetypes=[("PKG Files", "*.pkg"), ("Tous les fichiers", "*.*")])
        if filename:
            self.source_path = filename
            self.lbl_src.config(text=f"Source (Split) : {filename}")
            self.mode = "SPLIT"

    def pick_source_dir(self):
        dirname = filedialog.askdirectory(title="Sélectionner le dossier contenant les parties .pkg.00...")
        if dirname:
            self.source_path = dirname
            self.lbl_src.config(text=f"Dossier Source (Merge) : {dirname}")
            self.mode = "MERGE"

    def pick_output_dir(self):
        dirname = filedialog.askdirectory(title="Sélectionner le dossier de destination")
        if dirname:
            self.output_path = dirname
            self.lbl_out.config(text=f"Sortie : {dirname}")

    def execute_action(self):
        if not self.source_path or not self.output_path:
            messagebox.showerror("Erreur", "Veuillez choisir une source et un emplacement de sortie !")
            return

        self.progress["value"] = 0
        self.root.update_idletasks()

        try:
            if hasattr(self, "mode") and self.mode == "SPLIT":
                self.split_pkg_action()
            else:
                self.merge_pkg_action()
        except Exception as e:
            messagebox.showerror("Erreur critique", str(e))
            self.lbl_status.config(text="Échec du traitement.")

    def split_pkg_action(self):
        file_size = os.path.getsize(self.source_path)
        base_name = os.path.basename(self.source_path)
        clean_name = base_name[:-4] if base_name.lower().endswith(".pkg") else base_name
        max_part_size = 4 * 1024 * 1024 * 1024  # 4 Go

        self.lbl_status.config(text="Découpage en cours...")
        self.root.update_idletasks()

        with open(self.source_path, "rb") as f_in:
            part_index = 0
            transferred = 0
            while transferred < file_size:
                part_name = f"{clean_name}.pkg.{part_index:02d}"
                part_path = os.path.join(self.output_path, part_name)
                target_size = min(max_part_size, file_size - transferred)

                with open(part_path, "wb") as f_out:
                    remaining = target_size
                    while remaining > 0:
                        chunk = f_out.write(f_in.read(min(remaining, 16 * 1024 * 1024)))
                        if not chunk:
                            break
                        remaining -= 16 * 1024 * 1024
                        transferred += 16 * 1024 * 1024
                        pct = int((transferred / file_size) * 100)
                        self.progress["value"] = min(pct, 100)
                        self.root.update_idletasks()

                part_index += 1

        self.progress["value"] = 100
        self.lbl_status.config(text="Découpage réussi !")
        messagebox.showinfo("Succès", "Le découpage s'est déroulé avec succès !")

    def merge_pkg_action(self):
        parts = sorted([os.path.join(self.source_path, f) for f in os.listdir(self.source_path) if ".pkg." in f.lower() or f.endswith(".00")])
        if not parts:
            messagebox.showerror("Erreur", "Aucune partie de PKG trouvée dans ce dossier.")
            return

        output_file = os.path.join(self.output_path, "merged_game.pkg")
        total_size = sum(os.path.getsize(p) for p in parts)
        transferred = 0

        self.lbl_status.config(text="Fusion en cours...")
        self.root.update_idletasks()

        with open(output_file, "wb") as f_out:
            for part in parts:
                with open(part, "rb") as f_in:
                    while True:
                        data = f_in.read(16 * 1024 * 1024)
                        if not data:
                            break
                        f_out.write(data)
                        transferred += len(data)
                        pct = int((transferred / total_size) * 100)
                        self.progress["value"] = min(pct, 100)
                        self.root.update_idletasks()

        self.progress["value"] = 100
        self.lbl_status.config(text="Fusion réussie !")
        messagebox.showinfo("Succès", f"Fusion réussie !\nEnregistré sous : {output_file}")

if __name__ == "__main__":
    root = tk.Tk()
    app = PkgHubApp(root)
    root.mainloop()
