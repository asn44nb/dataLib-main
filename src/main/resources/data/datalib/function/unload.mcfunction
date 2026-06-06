# DataLib Engine - Unload
# Cleans up dataLib engine state

data remove storage datalib:engine loaded
scoreboard players reset #engine datalib.loaded

tellraw @a[permission_level=2] {"text":"[DataLib] ","color":"yellow","extra":[{"text":"Engine unloaded.","color":"white"}]}
