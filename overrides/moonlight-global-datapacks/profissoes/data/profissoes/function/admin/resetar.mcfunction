# Uso (admin): /execute as <jogador> run function profissoes:admin/resetar
# Remove a profissão, o mod inicial e as etapas do jogador; ele volta a ser Vagabundo e recebe a Carta de Apresentação
rpessentials license revoke @s engenheiro
rpessentials license revoke @s arcanista
rpessentials license revoke @s ferreiro
rpessentials license revoke @s naturalista
smpprofissoes resetar @s
tag @s remove prof.engenheiro
tag @s remove prof.arcanista
tag @s remove prof.ferreiro
tag @s remove prof.naturalista
tag @s remove prof.escolhida
tag @s remove prof.vagabundo
tag @s remove prof.escolhendo
scoreboard players reset @s prof.timer
