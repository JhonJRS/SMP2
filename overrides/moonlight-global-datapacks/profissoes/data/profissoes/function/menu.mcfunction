# Menu principal de escolha de profissão
title @s times 10 80 20
title @s title {"text":"Escolha sua Profissão","color":"gold","bold":true}
title @s subtitle {"text":"Abra o chat (T) e clique em uma opção","color":"gray"}
playsound minecraft:block.note_block.chime master @s ~ ~ ~ 1 1

tellraw @s {"text":"\n\n\n\n\n\n"}
tellraw @s {"text":"═══════════ ESCOLHA SUA PROFISSÃO ═══════════","color":"gold","bold":true}
tellraw @s {"text":"Esta escolha é permanente. Você só poderá fabricar itens dos mods da sua profissão (os demais mods são livres para todos).","color":"gray","italic":true}
tellraw @s {"text":""}
tellraw @s ["  ",{"text":"[⚙ Engenheiro]","color":"gold","bold":true,"clickEvent":{"action":"run_command","value":"/trigger prof.escolha set 1"},"hoverEvent":{"action":"show_text","contents":[{"text":"Engenheiro\n","color":"gold","bold":true},{"text":"Escolha começar por Create, Immersive Engineering ou Embers. O resto (addons e os outros mods) vem pelas pesquisas da colônia","color":"gray"}]}},{"text":"  começa por Create, IE ou Embers","color":"gray"}]
tellraw @s ["  ",{"text":"[✦ Arcanista]","color":"dark_purple","bold":true,"clickEvent":{"action":"run_command","value":"/trigger prof.escolha set 2"},"hoverEvent":{"action":"show_text","contents":[{"text":"Arcanista\n","color":"dark_purple","bold":true},{"text":"Escolha começar por Iron's Spells, Malum, Botania ou Neo Vitae. O resto vem pelas pesquisas da colônia","color":"gray"}]}},{"text":"  começa por Iron's Spells, Malum, Botania ou Neo Vitae","color":"gray"}]
tellraw @s ["  ",{"text":"[⚒ Ferreiro]","color":"red","bold":true,"clickEvent":{"action":"run_command","value":"/trigger prof.escolha set 3"},"hoverEvent":{"action":"show_text","contents":[{"text":"Ferreiro\n","color":"red","bold":true},{"text":"Escolha começar por Silent Gear (com a fundição do Productive Metalworks) ou Forbidden Arcanus. O resto vem pelas pesquisas da colônia","color":"gray"}]}},{"text":"  começa por Silent Gear + Productive Metalworks ou Forbidden Arcanus","color":"gray"}]
tellraw @s ["  ",{"text":"[❀ Naturalista]","color":"green","bold":true,"clickEvent":{"action":"run_command","value":"/trigger prof.escolha set 4"},"hoverEvent":{"action":"show_text","contents":[{"text":"Naturalista\n","color":"green","bold":true},{"text":"Escolha começar por Farmer's Delight, Farm & Charm ou Herbalismo. Vinhos, toxinas e alquimia vêm pelas pesquisas da colônia","color":"gray"}]}},{"text":"  começa por Farmer's Delight, Farm & Charm ou Herbalismo","color":"gray"}]
tellraw @s {"text":""}
tellraw @s {"text":"Clique na profissão; em seguida escolha o mod inicial e confirme.","color":"yellow"}
tellraw @s {"text":"═════════════════════════════════════════","color":"gold","bold":true}
