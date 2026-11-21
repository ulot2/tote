package app.tote.core.data.db

import app.tote.core.model.Entry
import app.tote.core.model.Profile

internal fun EntryEntity.toModel() = Entry(
    id = id,
    label = label,
    value = value,
    type = type,
    category = category,
    icon = icon,
    iconOverridden = iconOverridden,
    keywords = keywords.split('\n').filter { it.isNotBlank() },
    profileIds = profileIds.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet(),
    pinned = pinned,
    sortOrder = sortOrder,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Entry.toEntity() = EntryEntity(
    id = id,
    label = label.trim(),
    value = value,
    type = type,
    category = category,
    icon = icon,
    iconOverridden = iconOverridden,
    keywords = keywords.map { it.trim().replace('\n', ' ') }.filter { it.isNotEmpty() }.distinct().joinToString("\n"),
    profileIds = profileIds.sorted().joinToString(","),
    pinned = pinned,
    sortOrder = sortOrder,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ProfileEntity.toModel() = Profile(id, name, accentArgb, sortOrder)
internal fun Profile.toEntity() = ProfileEntity(id, name.trim(), accentArgb, sortOrder)
