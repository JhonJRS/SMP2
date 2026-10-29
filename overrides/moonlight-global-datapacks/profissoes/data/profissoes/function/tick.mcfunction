# Primeira entrada: inventário limpo + mochila (só quem nunca passou pelo sistema de profissões; quem já estava no mundo
# antes desta regra só ganha a tag, sem perder nada)
execute as @a[tag=!prof.chegou,tag=!prof.escolhida,tag=!prof.vagabundo,tag=!prof.isento] run function profissoes:chegada
tag @a[tag=!prof.chegou] add prof.chegou
# Quem ainda não escolheu profissão (e não é isento) é Vagabundo: recebe a Carta de Apresentação uma vez e escolhe quando quiser
execute as @a[tag=!prof.escolhida,tag=!prof.isento,tag=!prof.vagabundo] run function profissoes:vagabundo
# O menu de escolha só aparece para quem usou a carta
execute as @a[tag=prof.escolhendo,tag=!prof.escolhida,tag=!prof.isento] at @s run function profissoes:pendente
