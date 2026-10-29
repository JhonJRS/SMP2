# Jogador sem profissão está no estado Vagabundo (só a tag prof.vagabundo; não é licença): não acessa nenhum mod das
# profissões e leva a Carta de Apresentação para escolher uma
tag @s add prof.vagabundo
give @s smpprofissoes:guild_letter 1
tellraw @s [{"text":"Você é um ","color":"gray"},{"text":"Vagabundo","color":"white","bold":true},{"text":": ainda não pertence a nenhuma guilda. Use a ","color":"gray"},{"text":"Carta de Apresentação","color":"gold"},{"text":" (clique direito) quando quiser escolher sua profissão.","color":"gray"}]