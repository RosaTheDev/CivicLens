import { useEffect, useState } from 'react'
import { TextInput, Button, Stack, Card, Group, Text, Badge, Loader, Avatar } from '@mantine/core'
import { Link } from 'react-router-dom'

type Representative = {
  id: number
  name: string
  chamber: string
  state: string
  district?: string | null
  party?: string | null
  photoUrl?: string | null
}

export default function DashboardPage() {
  const [zip, setZip] = useState('')
  const [submittedZip, setSubmittedZip] = useState('')
  const [representatives, setRepresentatives] = useState<Representative[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const savedZip = sessionStorage.getItem('civiclens_lastZip')
    const savedReps = sessionStorage.getItem('civiclens_lastReps')
    if (savedZip && savedReps) {
      setZip(savedZip)
      setSubmittedZip(savedZip)
      try {
        const parsed: Representative[] = JSON.parse(savedReps)
        setRepresentatives(parsed)
      } catch {
        // ignore parse errors
      }
    }
  }, [])

  async function search(zipToUse: string) {
    const trimmed = zipToUse.trim()
    setSubmittedZip(trimmed)
    if (!trimmed) {
      setRepresentatives([])
      setError(null)
       sessionStorage.removeItem('civiclens_lastZip')
       sessionStorage.removeItem('civiclens_lastReps')
      return
    }
    try {
      setLoading(true)
      setError(null)
      const res = await fetch(`/api/representatives?zip=${encodeURIComponent(trimmed)}`)
      if (!res.ok) {
        throw new Error(`Request failed with status ${res.status}`)
      }
      const data: Representative[] = await res.json()
      setRepresentatives(data)
      sessionStorage.setItem('civiclens_lastZip', trimmed)
      sessionStorage.setItem('civiclens_lastReps', JSON.stringify(data))
    } catch (e: any) {
      setError(e.message ?? 'Failed to load representatives')
      setRepresentatives([])
      sessionStorage.removeItem('civiclens_lastZip')
      sessionStorage.removeItem('civiclens_lastReps')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack gap="md">
      <div className="civic-hero">
        <Text fw={700} size="xl" className="civic-heading">Find your representatives</Text>
        <Text size="sm" c="dimmed">Search by ZIP code to see who represents you and explore their details.</Text>
      </div>
      <Group className="civic-control-bar">
        <TextInput
          placeholder="Enter ZIP code"
          value={zip}
          onChange={(e) => setZip(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search(zip)}
        />
        <Button radius="xl" onClick={() => search(zip)} className="civic-action-button">Search</Button>
      </Group>
      {loading && <Loader size="sm" />}
      {error && <Text c="red">{error}</Text>}
      {submittedZip && !loading && representatives.length === 0 && (
        <Text c="dimmed">No representatives found for this ZIP. Try 94110 or 10001 for demo data.</Text>
      )}
      <Stack gap="sm">
        {representatives.map((rep) => (
          <Card
            key={rep.id}
            withBorder
            padding="md"
            radius="md"
            shadow="xs"
            className="civic-glass-card civic-rep-card"
            component={Link}
            to={`/representatives/${rep.id}`}
            style={{ textDecoration: 'none', color: 'inherit' }}
          >
            <Group justify="space-between" className="civic-list-card-body">
              <Group gap="sm" align="flex-start">
                <Avatar src={rep.photoUrl ?? undefined} radius="xl" color="blue">
                  {rep.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase()}
                </Avatar>
                <div>
                  <Text fw={600}>{rep.name}</Text>
                  <Group gap="xs" className="civic-list-meta">
                    <Badge size="sm" variant="light" className="civic-badge civic-badge--chamber">{rep.chamber}</Badge>
                    <Badge size="sm" variant="outline" className="civic-badge civic-badge--state">{rep.state}</Badge>
                    {rep.district && <Text size="sm" c="dimmed">District {rep.district}</Text>}
                    {rep.party && <Text size="sm" c="dimmed">{rep.party}</Text>}
                  </Group>
                  <div className="civic-list-divider" />
                  <Text size="sm" c="dimmed">
                    Open profile to review recent bills, donor context, and your stance.
                  </Text>
                </div>
              </Group>
            </Group>
          </Card>
        ))}
      </Stack>
    </Stack>
  )
}
