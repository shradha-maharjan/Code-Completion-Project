import pickle
filename = '/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/spt-code/vocabs_original/vocabs/ast/ast.pk'

ast_vocab = ''

with open(filename, 'rb') as file:
    ast_vocab = pickle.load(file)

print(len(ast_vocab))

