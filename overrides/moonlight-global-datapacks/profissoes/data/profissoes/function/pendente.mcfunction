# Executado a cada tick para quem usou a Carta de Apresentação e ainda não escolheu (o Vagabundo anda livremente)

# Libera o /trigger para os botões do chat funcionarem
scoreboard players enable @s prof.escolha

# Cliques nos botões: 1..4 profissão → 11..49 mod inicial (10*profissão+opção) → 111..149 confirmação; 5 volta ao menu
execute if score @s prof.escolha matches 1..4 run scoreboard players set @s prof.timer 41
execute if score @s prof.escolha matches 1..4 run function profissoes:selecionar
execute if score @s prof.escolha matches 5 run function profissoes:menu
execute if score @s prof.escolha matches 11..49 run scoreboard players set @s prof.timer 41
execute if score @s prof.escolha matches 11..49 run function profissoes:escolher_inicio
execute if score @s prof.escolha matches 111..149 run function profissoes:confirmar
scoreboard players set @s prof.escolha 0

# Mostra o menu logo depois de usar a carta e repete a cada 30s (os submenus reiniciam a contagem)
scoreboard players add @s prof.timer 1
execute if score @s prof.timer matches 40 run function profissoes:menu
execute if score @s prof.timer matches 640.. run scoreboard players set @s prof.timer 39
