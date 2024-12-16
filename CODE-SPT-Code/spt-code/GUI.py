import tkinter as tk
from tkinter import ttk
import javalang
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch
import re


class CodeCompletionApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Code Completion with Probabilities")
        self.root.geometry("900x600")

        self.main_container = tk.Frame(root)
        self.main_container.pack(fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(self.main_container, highlightthickness=0)
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.scrollbar = ttk.Scrollbar(self.main_container, orient=tk.VERTICAL, command=self.canvas.yview)
        self.scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.canvas.configure(yscrollcommand=self.scrollbar.set)
        self.canvas.bind("<Configure>", lambda e: self.canvas.itemconfig(self.canvas_window, width=e.width))

        self.scrollable_frame = tk.Frame(self.canvas)
        self.canvas_window = self.canvas.create_window((0, 0), window=self.scrollable_frame, anchor="nw")

        self.scrollable_frame.bind(
            "<Configure>", lambda e: self.canvas.configure(scrollregion=self.canvas.bbox("all"))
        )

        self.model_name = "shradha01/llm-coding-tasks-model"
        self.tokenizer_name = "shradha01/llm-coding-tasks-tokenizer"
        self.load_model_and_tokenizer()

        # Replace the main_frame with a PanedWindow for resizable borders
        self.paned_window = tk.PanedWindow(self.scrollable_frame, orient=tk.HORIZONTAL, sashwidth=5)
        self.paned_window.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # Left frame inside the paned window
        self.left_frame = tk.Frame(self.paned_window, width=450)
        self.paned_window.add(self.left_frame)

        self.input_frame = tk.Frame(self.left_frame)
        self.input_frame.pack(fill=tk.BOTH, expand=True)

        self.input_label = tk.Label(self.input_frame, text="Input Code:", font=("Arial", 12))
        self.input_label.pack(anchor="w", pady=(0, 5))

        self.input_scrollbar = tk.Scrollbar(self.input_frame, orient=tk.VERTICAL)
        self.input_text = tk.Text(
            self.input_frame,
            height=25,
            wrap=tk.WORD,
            font=("Arial", 12),
            yscrollcommand=self.input_scrollbar.set
        )
        self.input_scrollbar.config(command=self.input_text.yview)
        self.input_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.input_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # self.extract_button = ttk.Button(self.left_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
        # self.extract_button.pack(pady=10)

        # Add a nested PanedWindow for the right frame with vertical resizing
        self.right_paned_window = tk.PanedWindow(self.paned_window, orient=tk.VERTICAL, sashwidth=5)
        self.paned_window.add(self.right_paned_window)
        
        # Frame with border and grey background for "Select an Option:"
        # Add a label for "Select an Option:"
        self.options_label = tk.Label(
            self.right_paned_window,
            text="Select an Option:",
            font=("Arial", 12),
            fg="black",
            anchor="w"
        )
        self.right_paned_window.add(self.options_label)

        self.options_frame_box = tk.Frame(self.right_paned_window, bg="#f6f6f6", bd=0, relief="flat")
        self.right_paned_window.add(self.options_frame_box)

        self.options_canvas = tk.Canvas(self.options_frame_box, bg="#f6f6f6", highlightthickness=0)
        self.options_scrollbar = tk.Scrollbar(self.options_frame_box, orient=tk.VERTICAL, command=self.options_canvas.yview)
        self.options_inner_frame = tk.Frame(self.options_canvas, bg="#f6f6f6")

        self.options_canvas.configure(yscrollcommand=self.options_scrollbar.set)
        self.options_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.options_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.options_canvas_window = self.options_canvas.create_window((0, 0), window=self.options_inner_frame, anchor="nw")

        self.options_inner_frame.bind(
            "<Configure>",
            lambda e: self.options_canvas.configure(scrollregion=self.options_canvas.bbox("all"))
        )

        self.selected_option = tk.StringVar(value="")
        self.options = {}
        self.option_buttons = []

        # Bottom section of the right frame (output)
        self.output_frame = tk.Frame(self.right_paned_window)
        self.right_paned_window.add(self.output_frame)

        self.output_label = tk.Label(self.output_frame, text="Generated Candidates:", font=("Arial", 12))
        self.output_label.pack(anchor="w", pady=(10, 5))

        self.output_scrollbar = tk.Scrollbar(self.output_frame, orient=tk.VERTICAL)
        self.output_text = tk.Text(
            self.output_frame,
            height=15,
            wrap=tk.WORD,
            font=("Arial", 12),
            state=tk.DISABLED,
            yscrollcommand=self.output_scrollbar.set
        )
        self.output_scrollbar.config(command=self.output_text.yview)
        self.output_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.output_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # self.output_label = tk.Label(self.right_frame, text="Generated Candidates:", font=("Arial", 12))
        # self.output_label.pack(anchor="w", pady=(10, 5))

        # self.output_text = tk.Text(self.right_frame, height=15, wrap=tk.WORD, font=("Arial", 12), state=tk.DISABLED)
        # self.output_text.pack(fill=tk.BOTH, expand=True)

        self.bottom_frame = tk.Frame(self.scrollable_frame)
        self.bottom_frame.pack(fill=tk.X, padx=10, pady=10)

        self.extract_button = ttk.Button(self.bottom_frame, text="Extract Masked Methods", command=self.extract_masked_methods)
        self.extract_button.grid(row=0, column=0, padx=5, pady=5, sticky="ew")

        self.model_info_button = ttk.Button(self.bottom_frame, text="Model Info", command=self.show_model_info)
        self.model_info_button.grid(row=0, column=1, padx=5, pady=5, sticky="ew")

        self.model_name_dropdown = ttk.Combobox(
            self.bottom_frame,
            state="readonly",
            values=["shradha01/llm-coding-tasks-model", "other-model-1"]
        )
        self.model_name_dropdown.set(self.model_name)
        self.model_name_dropdown.grid(row=0, column=2, padx=5, pady=5, sticky="ew")

        self.generate_candidate_button = ttk.Button(self.bottom_frame, text="Generate Candidates", command=self.generate_candidates)
        self.generate_candidate_button.grid(row=0, column=3, padx=5, pady=5, sticky="ew")

        # Adjust the column configuration for even spacing
        self.bottom_frame.columnconfigure(0, weight=1)
        self.bottom_frame.columnconfigure(1, weight=1)
        self.bottom_frame.columnconfigure(2, weight=2)
        self.bottom_frame.columnconfigure(3, weight=1)
        #self.model_name_dropdown.grid(row=0, column=1, padx=5, pady=5, sticky="ew")


    def load_model_and_tokenizer(self):
        try:
            self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
            self.tokenizer = AutoTokenizer.from_pretrained(self.tokenizer_name)
            print("Model and tokenizer loaded successfully!")
        except Exception as e:
            print(f"Error loading model or tokenizer: {e}")
            self.model = None
            self.tokenizer = None
    
    # def extract_masked_methods(self):
    #     input_code = self.input_text.get("1.0", tk.END).strip()
    #     if not input_code:
    #         self.display_output("Please enter Java code in the input field.")
    #         return

    #     try:
    #         # Parse the Java code into an AST
    #         tree = javalang.parse.parse(input_code)
    #     except javalang.parser.JavaSyntaxError as e:
    #         self.display_output(f"Syntax error in Java code: {e}")
    #         return
    #     except Exception as e:
    #         self.display_output(f"Error parsing Java code: {e}")
    #         return

    #     self.options.clear()
    #     for widget in self.options_frame.winfo_children():
    #         widget.destroy()
    #     self.option_buttons = []

    #     idx = 0
    #     for path, node in tree:
    #         if isinstance(node, javalang.tree.MethodDeclaration):
    #             # Construct the method signature
    #             modifiers = " ".join(node.modifiers)
    #             return_type = node.return_type.name if node.return_type else "void"
    #             parameters = ", ".join(
    #                 f"{p.type.name} {p.name}" for p in node.parameters
    #             )
    #             method_signature = f"{modifiers} {return_type} {node.name}({parameters})"

    #             # Check for [MSK] in the method body
    #             if node.body:
    #                 method_body = "".join(token.value for token in node.body if hasattr(node, 'body'))
    #                 if "[MSK]" in method_body:
    #                     idx += 1
    #                     option_name = f"Option {idx}"
    #                     method_code = f"{method_signature} {{ {method_body} }}"
    #                     self.options[option_name] = method_code.strip()

    #                     # Add a radio button for the method
    #                     button = tk.Radiobutton(
    #                         self.options_frame,
    #                         text=f"{method_signature}...",
    #                         variable=self.selected_option,
    #                         value=option_name,
    #                         font=("Arial", 10),
    #                         justify=tk.LEFT,
    #                         wraplength=400
    #                     )
    #                     button.pack(anchor="w", pady=5)
    #                     self.option_buttons.append(button)

    #     if self.options:
    #         self.selected_option.set(next(iter(self.options)))  # Select the first option by default
    #         self.display_output(f"Extracted {len(self.options)} method(s) with '[MSK]'.")
    #     else:
    #         self.display_output("No masked methods found in the input code.")

    def extract_masked_methods(self):
        input_code = self.input_text.get("1.0", tk.END).strip()
        if not input_code:
            self.display_output("Please enter Java code in the input field.")
            return

        method_pattern = re.compile(
            r'(?:@\w+(?:\s*\([^)]*\))?\s*)*'  # Annotations
            r'((?:public|private|protected|static|final|synchronized|abstract|native|strictfp|transient|volatile)?\s*'  # Modifiers
            r'(?:<[^>]+>\s*)?'  # Generic types
            r'(?:[\w\[\]]+\s+)+'  # Return type
            r'\w+\s*\(.*?\)\s*'  # Method name and parameters
            r'(?:throws\s+\w+(?:\s*,\s*\w+)*)?\s*'  # Exceptions
            #r'\{(?:[^{}]*|\{.*?\})*\})',  # Method body with nested braces
            #r'\{(?:[^{}]*|\{.*?\}|.*?)*?\})',# Method body allowing placeholders
            r'\{(?:[^{}]|\{(?:[^{}]*|\{.*?\})*\})*\})',  # Match nested braces
            re.DOTALL  
        )

        matches = method_pattern.finditer(input_code)

        self.options.clear()
        all_methods = []  
        masked_methods = [] 
        for widget in self.options_inner_frame.winfo_children():
            widget.destroy()
        self.option_buttons = []

        for match in matches:
            method_code = match.group(1).strip()
            all_methods.append(method_code)  

            if "[MSK]" in method_code:
                option_name = f"Option {len(masked_methods) + 1}"
                self.options[option_name] = method_code
                masked_methods.append(method_code)

                button = tk.Radiobutton(
                    self.options_inner_frame,
                    text=method_code,
                    variable=self.selected_option,
                    value=option_name,
                    font=("Arial", 10),
                    bg="#f6f6f6",
                    anchor="w",
                    justify=tk.LEFT,
                    wraplength=400
                )
                button.pack(anchor="w", pady=5)
                self.option_buttons.append(button)

        if self.options:
            self.selected_option.set(next(iter(self.options)))  
        else:
            self.display_output(f"Extracted {len(all_methods)} method(s) but no method contains '[MSK]'.")


    def generate_candidates(self):
        selected_option_name = self.selected_option.get()
        selected_code = self.options.get(selected_option_name, "")

        if not selected_code:
            self.display_output("Please select a valid option.")
            return

        # input_code = self.input_text.get("1.0", tk.END).strip()
        # full_code = input_code + "\n\n" + selected_code

        if not self.model or not self.tokenizer:
            self.display_output("Model or tokenizer not loaded.")
            return

        self.display_output("Generating candidates... Please wait.")

        try:
            encoded = self.tokenizer(selected_code, return_tensors="pt")
            outputs = self.model.generate(
                encoded["input_ids"],
                max_length=100,
                num_beams=5,
                num_return_sequences=5,
                return_dict_in_generate=True,
                output_scores=True
            )

            decoded_candidates = [
                self.tokenizer.decode(output, skip_special_tokens=True)
                for output in outputs.sequences
            ]

            probs = [
                torch.exp(score).item()
                for score in outputs.sequences_scores
            ]

            result_text = ""
            for idx, (candidate, prob) in enumerate(zip(decoded_candidates, probs)):
                result_text += f"Candidate {idx + 1}: Probability: {prob:.4f}, {candidate.strip()}\n"

            self.display_output(result_text)

        except Exception as e:
            self.display_output(f"Error during prediction: {e}")

    def display_output(self, text):
        print(text)
        self.output_text.config(state=tk.NORMAL)
        self.output_text.delete("1.0", tk.END)
        self.output_text.insert(tk.END, text)
        self.output_text.config(state=tk.DISABLED)

    def show_model_info(self):
        info = f"Model: {self.model_name}\nTokenizer: {self.tokenizer_name}"
        self.display_output(info)


if __name__ == "__main__":
    root = tk.Tk()
    app = CodeCompletionApp(root)
    root.mainloop()