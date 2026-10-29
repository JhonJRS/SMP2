# Profissão e mod inicial aplicados (chamado por profissoes:inicio/<mod>): libera o jogador
tag @s add prof.escolhida
# deixa de ser Vagabundo: tira a marca e a carta
tag @s remove prof.vagabundo
tag @s remove prof.escolhendo
clear @s smpprofissoes:guild_letter
scoreboard players reset @s prof.timer

title @s times 10 60 20
title @s title {"text":"Profissão escolhida!","color":"green","bold":true}
execute if entity @s[tag=prof.engenheiro] run title @s subtitle {"text":"Você agora é Engenheiro","color":"gold"}
execute if entity @s[tag=prof.arcanista] run title @s subtitle {"text":"Você agora é Arcanista","color":"dark_purple"}
execute if entity @s[tag=prof.ferreiro] run title @s subtitle {"text":"Você agora é Ferreiro","color":"red"}
execute if entity @s[tag=prof.naturalista] run title @s subtitle {"text":"Você agora é Naturalista","color":"green"}
tellraw @s [{"text":"Os outros mods da sua profissão são liberados pelas pesquisas da aba ","color":"gray"},{"text":"Guild of Trades","color":"gold"},{"text":" na Universidade da sua colônia. Use ","color":"gray"},{"text":"/progression","color":"yellow"},{"text":" para ver o que falta.","color":"gray"}]
playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1
