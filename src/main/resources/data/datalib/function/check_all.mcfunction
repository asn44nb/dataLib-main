# DataLib Engine - check_all
# Validates that the dataLib engine is loaded and all dependent datapacks are functional.
# This function is called by dependent datapacks via the datalib:check_all function tag.
# Each dependent datapack appends its own check function to the tag.

# Verify engine storage
execute store result score #check datalib.loaded run data get storage datalib:engine loaded
execute unless score #check datalib.loaded matches 1 run tellraw @a[permission_level=2] {"text":"[DataLib] ","color":"red","extra":[{"text":"Engine storage not found! Reinitializing...","color":"white"}]}
execute unless score #check datalib.loaded matches 1 run function datalib:init

# Run all registered checks from the function tag
# (dependent datapacks add their check functions to datalib:check_all tag)
