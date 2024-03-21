from huggingface_hub import Repository, get_full_repo_name
from huggingface_hub import notebook_login
from huggingface_hub import login

login("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")


model_name = "codeparrot-ds-accelerate"
repo_name = get_full_repo_name(model_name)
repo_name

output_dir = "codeparrot-ds-accelerate"
repo = Repository(output_dir, clone_from=repo_name)

print("----repo----")
print(repo)

